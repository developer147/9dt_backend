package com._98point6.droptoken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._98point6.droptoken.model.CreateGameRequest;
import com._98point6.droptoken.model.CreateGameResponse;
import com._98point6.droptoken.model.GameStatusResponse;
import com._98point6.droptoken.model.GetGamesResponse;
import com._98point6.droptoken.model.GetMoveResponse;
import com._98point6.droptoken.model.GetMovesResponse;
import com._98point6.droptoken.model.PostMoveRequest;
import com._98point6.droptoken.model.PostMoveResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 */
@Path("/drop_token")
@Produces(MediaType.APPLICATION_JSON)
public class DropTokenResource {
    private static final Logger logger = LoggerFactory.getLogger(DropTokenResource.class);
    private GetGamesResponse inProgressGames;
    private Map<String, GameStatusResponse> allGames;

    public DropTokenResource() {
    	GetGamesResponse.Builder getGamesResponseBuilder = new GetGamesResponse.Builder();
    	inProgressGames = getGamesResponseBuilder.games(new ArrayList<String>())
    		   .build();
    	allGames = new HashMap<String, GameStatusResponse>();
    }
    

    @GET
    public Response getGames() {
        //return Response.ok(new GetGamesResponse()).build();
    	return Response.ok(inProgressGames.getGames()).build();
    }

    @POST
    public Response createNewGame(CreateGameRequest request) {
        logger.info("request={}", request);
        
        if (!validateMinPlayers(request) || !validateMinBoardSize(request)) {
        	return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        int currentGames = inProgressGames.getGames().size();
        String newGame = Integer.toString(currentGames + 1);
        
        addToAllGames(newGame, request);

        //return Response.ok(new CreateGameResponse()).build();
        return Response.ok(addToInProgressGames(newGame)).build();
    }

    @Path("/{id}")
    @GET
    public Response getGameStatus(@PathParam("id") String gameId) {
        logger.info("gameId = {}", gameId);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);
        
        if (gameStatusResponse == null) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        String json = getGameStatusResponse(gameStatusResponse);
        
        //return Response.ok(gameStatusResponse).build();
        return Response.ok(json).build();
    }

    @Path("/{id}/{playerId}")
    @POST
    public Response postMove(@PathParam("id")String gameId, @PathParam("playerId") String playerId, PostMoveRequest request) {
        logger.info("gameId={}, playerId={}, move={}", gameId, playerId, request);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);
        
        if (!inProgressGames.getGames().contains(gameId) || 
        		!gameStatusResponse.getPlayers().contains(playerId)) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        if (request.getColumn() < 1 || request.getColumn() > gameStatusResponse.getColumns()) {
        	return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        if (!gameStatusResponse.getPlayers().get(gameStatusResponse.getTurn()).equals(playerId)) {
        	return Response.status(Response.Status.CONFLICT).build();
        } 
        
        int rowIndex = 0;
        while (!gameStatusResponse.getGrid(rowIndex, request.getColumn()).isEmpty()) {
        	rowIndex++;
            if (rowIndex == gameStatusResponse.getRows()) {
            	return Response.status(Response.Status.BAD_REQUEST).build();
            } 
        }
        gameStatusResponse.setGrid(rowIndex, request.getColumn(), playerId);
        gameStatusResponse.incrementMoves();
        
        PostMoveResponse.Builder postMoveResponseBuilder = new PostMoveResponse.Builder();
        PostMoveResponse postMoveResponse = postMoveResponseBuilder.moveLink(gameId + "/moves/" + gameStatusResponse.getMoves())
        					   .build();
        
        //TODO: check for a win or a draw and stop the game if yes is an answer to either of the above
        if (didPlayerWin(gameStatusResponse, rowIndex, request.getColumn(), playerId, gameId) ||
        		isGameDrawn(gameStatusResponse)) {
        	GameStatusResponse.Builder builder = new GameStatusResponse.Builder();
        	builder = builder.fromPrototype(gameStatusResponse);
        	GameStatusResponse newGameStatusResponse = builder.state("DONE").build();
        	allGames.put(gameId, newGameStatusResponse);
        	
        	for (int index = 0; index < inProgressGames.getGames().size(); index++) {
        		if (inProgressGames.getGames().get(index).equals(gameId)) {
        			inProgressGames.getGames().remove(index);
        			break;
        		}
        	}
        	return Response.ok(postMoveResponse).build();
        }
        
        gameStatusResponse.setNextTurn();
        
//        GameStatusResponse.Builder builder = new GameStatusResponse.Builder().fromPrototype(gameStatusResponse);
//        GameStatusResponse newGameStatusResponse = builder.moves(gameStatusResponse.getMoves() + 1).build();
//        allGames.put(gameId, newGameStatusResponse);
        
        GetMoveResponse.Builder getMoveResponseBuilder = new GetMoveResponse.Builder();
        GetMoveResponse getMoveResponse = getMoveResponseBuilder.type("MOVE")
        					  .player(playerId)
        					  .column(request.getColumn())
        					  .build();
        
        List<GetMoveResponse> movesResponse = gameStatusResponse.getMovesResponse().getMoves();
        movesResponse.add(getMoveResponse);
        
        //return Response.ok(new PostMoveResponse()).build();
        return Response.ok(postMoveResponse).build();
    }

    @Path("/{id}/{playerId}")
    @DELETE
    public Response playerQuit(@PathParam("id")String gameId, @PathParam("playerId") String playerId) {
        logger.info("gameId={}, playerId={}", gameId, playerId);
        return Response.status(202).build();
    }
    @Path("/{id}/moves")
    @GET
    public Response getMoves(@PathParam("id") String gameId, @QueryParam("start") Integer start, @QueryParam("until") Integer until) {
        logger.info("gameId={}, start={}, until={}", gameId, start, until);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);
        
        if (start == null) {
        	start = 0;
        }
        if (until == null) {
        	until = gameStatusResponse.getMoves();
        }        
        
        if (start > until) {
        	return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if ((gameStatusResponse == null) || (start < 0) || until > gameStatusResponse.getMoves()) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        GetMovesResponse movesResponse = gameStatusResponse.getMovesResponse();
        
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        
        List<ObjectNode> nodes = new ArrayList<ObjectNode>();
        for (int index = 0; index < movesResponse.getMoves().size(); index++) {
        	if (index < start) {
        		continue;
        	}
        	if (index > until) {
        		break;
        	}
        	
        	ObjectNode node = mapper.createObjectNode();
        	GetMoveResponse moveResponse = movesResponse.getMoves().get(index);
        	node.put("type", moveResponse.getType());
        	node.put("player", moveResponse.getPlayer());
        	// TODO: take a closer look at optional column 
        	node.put("column", moveResponse.getColumn().get().toString());
        	nodes.add(node);
        }
        arrayNode.addAll(nodes);
        
        String json = null;
        try {
			json = mapper.writeValueAsString(arrayNode);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        //return Response.ok(new GetMovesResponse()).build();
        return Response.ok(json).build();
    }

    @Path("/{id}/moves/{moveId}")
    @GET
    public Response getMove(@PathParam("id") String gameId, @PathParam("moveId") Integer moveId) {
        logger.info("gameId={}, moveId={}", gameId, moveId);
        return Response.ok(new GetMoveResponse()).build();
    }
    
    private boolean validateMinPlayers(CreateGameRequest request) {
    	return request.getPlayers().size() >= 2;
    }
    
    private boolean validateMinBoardSize(CreateGameRequest request) {
    	return request.getColumns() >= 4 && request.getRows() >= 4;
    }
    
    private CreateGameResponse addToInProgressGames(String newGame) {
        inProgressGames.getGames().add(newGame);
        
        CreateGameResponse.Builder builder = new CreateGameResponse.Builder();
        CreateGameResponse gameResponse = builder.gameId(newGame)
        		.build();
        return gameResponse;
    }
    
    private void addToAllGames(String newGame, CreateGameRequest request) {
        GameStatusResponse.Builder gameStatusResponseBuilder = new GameStatusResponse.Builder();
        GameStatusResponse gameStatusResponse = 
			 gameStatusResponseBuilder.players(request.getPlayers())
									  //.moves(0)
									  .winner(null)
									  .state("IN_PROGRESS")
									  .columns(request.getColumns())
									  .rows(request.getRows()).build(); //better could be an enum
        allGames.put(newGame, gameStatusResponse);    	
    }
    
    private String getGameStatusResponse(GameStatusResponse gameStatusResponse) {
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode node = mapper.createObjectNode();
        
        node.put("players", gameStatusResponse.getPlayers().toString());
        node.put("state", gameStatusResponse.getState());
        if (gameStatusResponse.getState() == "DONE") {
        	node.put("winner", gameStatusResponse.getWinner().toString());
        }
        //TODO: Handle when winner attribute should be included in the response
        
        
        String json = null;
        try {
			json = mapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
        return json;
    }

    private boolean didPlayerWin(GameStatusResponse gameStatusResponse, int row, int column, String playerId, String gameId) {
    	// column check for win
    	//if (row == gameStatusResponse.getRows() - 1) {
		int rowIndex = 0;
    	while (rowIndex < gameStatusResponse.getRows()) {
    		if (!gameStatusResponse.getGrid(rowIndex, column).equals(playerId)) {
    			break;
    		}
    		rowIndex++;
    	}   
    	if (rowIndex == gameStatusResponse.getRows()) {
    		GameStatusResponse.Builder builder = new GameStatusResponse.Builder();
    		builder = builder.fromPrototype(gameStatusResponse);
    		GameStatusResponse newGameStatusResponse = builder.winner(playerId).build();
    		allGames.put(gameId, newGameStatusResponse);
    		return true;
    	}
    	//}
    	
    	int columnIndex = 0;
    	while (columnIndex < gameStatusResponse.getColumns()) {
    		if (!gameStatusResponse.getGrid(row, columnIndex).equals(playerId)) {
    			break;
    		}
    		columnIndex++;
    	}

    	if (columnIndex == gameStatusResponse.getColumns()) {
    		GameStatusResponse.Builder builder = new GameStatusResponse.Builder();
    		builder = builder.fromPrototype(gameStatusResponse);
    		GameStatusResponse newGameStatusResponse = builder.winner(playerId).build();
    		allGames.put(gameId, newGameStatusResponse);    		
    		return true;
    	}
    	
		return false;
    }
    
    private boolean isGameDrawn(GameStatusResponse gameStatusResponse) {
    	return gameStatusResponse.getMoves().equals(gameStatusResponse.getRows() * gameStatusResponse.getColumns())
    			|| gameStatusResponse.getPlayers().size() == 1;
    }
}
