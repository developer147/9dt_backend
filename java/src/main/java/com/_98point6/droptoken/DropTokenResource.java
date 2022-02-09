package com._98point6.droptoken;

import java.util.ArrayList;
import java.util.Arrays;
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
    	return Response.ok(inProgressGames.getGames()).build();
    }

    @POST
    public Response createNewGame(CreateGameRequest request) {
        logger.info("request={}", request);
        
        if (!validateMinPlayers(request) || !validateMinBoardSize(request) || !validateBoardDimensions(request)) {
        	return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        String newGame = Integer.toString(allGames.keySet().size() + 1);
        
        addToAllGames(newGame, request);

        return Response.ok(addToInProgressGames(newGame)).build();
    }

    @Path("/{id}")
    @GET
    public Response getGameStatus(@PathParam("id") String gameId) throws JsonProcessingException {
        logger.info("gameId = {}", gameId);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);
        
        if (gameStatusResponse == null) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        return Response.ok(getGameStatusResponse(gameStatusResponse)).build();
    }

    @Path("/{id}/{playerId}")
    @POST
    public Response postMove(@PathParam("id")String gameId, @PathParam("playerId") String playerId, PostMoveRequest request) {
        logger.info("gameId={}, playerId={}, move={}", gameId, playerId, request);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);

        
        if (!inProgressGames.getGames().contains(gameId)) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        int rows = gameStatusResponse.getGrid().length;
        int columns = gameStatusResponse.getGrid()[0].length;
        if (!gameStatusResponse.getPlayers().contains(playerId)) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        //if (request.getColumn() < 1 || request.getColumn() > gameStatusResponse.getGgetColumns()) {
        if (request.getColumn() < 0 || request.getColumn() >= columns) {
        	return Response.status(Response.Status.BAD_REQUEST).build();
        }
        
        //if (!gameStatusResponse.getPlayers().get(gameStatusResponse.getTurn()).equals(playerId)) {
        if (!gameStatusResponse.getWhoseTurn().equals(playerId)) {
        	return Response.status(Response.Status.CONFLICT).build();
        } 
        	
        int rowIndex = 0;
        while (!gameStatusResponse.getGridCell(rowIndex, request.getColumn()).isEmpty()) {
        	rowIndex++;
            if (rowIndex == rows) {
            	return Response.status(Response.Status.BAD_REQUEST).build();
            } 
        }
        gameStatusResponse.setGridCell(rowIndex, request.getColumn(), playerId);
        gameStatusResponse.incrementMoves();
        
        PostMoveResponse.Builder postMoveResponseBuilder = new PostMoveResponse.Builder();
        PostMoveResponse postMoveResponse = postMoveResponseBuilder.moveLink(gameId + "/moves/" + gameStatusResponse.getMoves())
        					   .build();
        
        GetMoveResponse.Builder getMoveResponseBuilder = new GetMoveResponse.Builder();
        GetMoveResponse getMoveResponse = getMoveResponseBuilder.type("MOVE")
        					  .player(playerId)
        					  .column(request.getColumn())
        					  .build();
        
        List<GetMoveResponse> movesResponse = gameStatusResponse.getMovesResponse().getMoves();
        movesResponse.add(getMoveResponse);        
        
        //TODO: check for a win or a draw and stop the game if yes is an answer to either of the above
        if (didPlayerWin(gameStatusResponse, rowIndex, request.getColumn(), playerId, gameId) ||
        		isGameDrawn(gameStatusResponse)) {
        	markGameAsDone(gameStatusResponse, playerId, gameId);
        } else {
        	gameStatusResponse.setWhoseTurn();
        }
        
        return Response.ok(postMoveResponse).build();
    }

    @Path("/{id}/{playerId}")
    @DELETE
    public Response playerQuit(@PathParam("id")String gameId, @PathParam("playerId") String playerId) {
        logger.info("gameId={}, playerId={}", gameId, playerId);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);
        
        if (gameStatusResponse != null && gameStatusResponse.getState() == "DONE") {
        	return Response.status(Response.Status.GONE).build();
        }
        
        if (!inProgressGames.getGames().contains(gameId) ||
        		!gameStatusResponse.getPlayers().contains(playerId)) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        List<String> players = gameStatusResponse.getPlayers();
        
        if (gameStatusResponse.getWhoseTurn().equals(playerId)) {
        	gameStatusResponse.setWhoseTurn();
        }
        
        for (int index = 0; index < players.size(); index++) {
        	if (players.get(index).equals(playerId)) {
        		System.out.println("AAAAAAAA");
        		players.remove(index);
        		break;
        	}
        }
        
        System.out.println("BBBBBBBBB");
        if (players.size() == 1) {
        	markGameAsDone(gameStatusResponse, players.get(0), gameId);
        }
        
        return Response.status(202).build();
    }
    
    @Path("/{id}/moves")
    @GET
    public Response getMoves(@PathParam("id") String gameId, @QueryParam("start") Integer start, @QueryParam("until") Integer until) throws JsonProcessingException {
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
        
        return Response.ok(mapper.writeValueAsString(arrayNode)).build();
    }

    @Path("/{id}/moves/{moveId}")
    @GET
    public Response getMove(@PathParam("id") String gameId, @PathParam("moveId") Integer moveId) {
        logger.info("gameId={}, moveId={}", gameId, moveId);
        
        GameStatusResponse gameStatusResponse = allGames.get(gameId);
        
        if (gameStatusResponse == null) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        List<GetMoveResponse> moves = gameStatusResponse.getMovesResponse().getMoves();
        if ((moveId < 1)|| (moveId > moves.size())) {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        return Response.ok(moves.get(moveId - 1)).build();
    }
    
    private boolean validateMinPlayers(CreateGameRequest request) {
    	return request.getPlayers().size() >= 2;
    }
    
    private boolean validateMinBoardSize(CreateGameRequest request) {
    	return request.getColumns() >= 4 && request.getRows() >= 4;
    }
    
    private boolean validateBoardDimensions(CreateGameRequest request) {
    	return request.getColumns() == request.getRows();
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
        
        GetMovesResponse.Builder getMovesResponseBuilder = new GetMovesResponse.Builder();
        GetMovesResponse movesResponse = getMovesResponseBuilder.moves(new ArrayList<GetMoveResponse>())
        									   .build();        
        
        GameStatusResponse gameStatusResponse = 
			 gameStatusResponseBuilder.players(request.getPlayers())
									  .moves(0)
									  .winner(null)
									  .whoseTurn(request.getPlayers().get(0))
									  .grid(initGrid(request.getRows(), request.getColumns()))
									  .movesResponse(movesResponse)
									  .state("IN_PROGRESS").build();
									  //.columns(request.getColumns())
									  //.rows(request.getRows()).build(); //better could be an enum
        allGames.put(newGame, gameStatusResponse);    	
    }
    
    private String[][] initGrid(int rows, int columns) {
        String[][] grid = new String[rows][columns];
        for (String[] row: grid) {
        	Arrays.fill(row, "");
        }
        return grid;
    }
    
    private String getGameStatusResponse(GameStatusResponse gameStatusResponse) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        
        ObjectNode node = mapper.createObjectNode();
        
        node.put("players", gameStatusResponse.getPlayers().toString());
        node.put("state", gameStatusResponse.getState());
        if (gameStatusResponse.getState() == "DONE") {
        	node.put("winner", gameStatusResponse.getWinner().get().toString());
        }
        
		return mapper.writeValueAsString(node);
    }

    private boolean didPlayerWin(GameStatusResponse gameStatusResponse, int row, int column, String playerId, String gameId) {
    	// column check for win
        if (contiguousColumnCheck(gameStatusResponse, column, playerId, gameId) ||
    		contiguousRowCheck(gameStatusResponse, row, playerId, gameId) ||
    		contiguousDiagnolCheck(gameStatusResponse, row, column, playerId, gameId) ||
    		continguousAntiDiagnolCheck(gameStatusResponse, row, column, playerId, gameId)) {
        	return true;
        }
    	
		return false;
    }
    
    private boolean contiguousDiagnolCheck(GameStatusResponse gameStatusResponse, int row, int column, String playerId, String gameId) {
    	if (row != column) {
    		return false;
    	}
    	
    	int rows = gameStatusResponse.getGrid().length;
    	int index = 0;
    	while (index < rows) {
    		if (!gameStatusResponse.getGridCell(index, index).equals(playerId)) {
    			return false;
    		}
    		index++;
    	}
    	
    	return true;
    }
    
    private boolean continguousAntiDiagnolCheck(GameStatusResponse gameStatusResponse, int row, int column, String playerId, String gameId) {
    	if ((row + column) != (gameStatusResponse.getGrid().length - 1)) {
    		return false;
    	}
    	
    	int rows = gameStatusResponse.getGrid().length;
    	int rowIndex = 0;
    	int colIndex = gameStatusResponse.getGrid()[0].length - 1;
    	while (rowIndex < rows) {
    		if (!gameStatusResponse.getGridCell(rowIndex, colIndex).equals(playerId)) {
    			return false;
    		}
    		rowIndex++;
    		colIndex--;
    	}
    	
    	return true;
    }
    
    private boolean contiguousColumnCheck(GameStatusResponse gameStatusResponse, int column, String playerId, String gameId) {
    	int rows = gameStatusResponse.getGrid().length;
		int rowIndex = 0;
    	while (rowIndex < rows) {
    		if (!gameStatusResponse.getGridCell(rowIndex, column).equals(playerId)) {
    			break;
    		}
    		rowIndex++;
    	}   
    	if (rowIndex == rows) {
    		return true;
    	}
    	return false;
    }
    
    private boolean contiguousRowCheck(GameStatusResponse gameStatusResponse, int row, String playerId, String gameId) {
    	int columns = gameStatusResponse.getGrid()[0].length;
    	int columnIndex = 0;
    	while (columnIndex < columns) {
    		if (!gameStatusResponse.getGridCell(row, columnIndex).equals(playerId)) {
    			break;
    		}
    		columnIndex++;
    	}

    	if (columnIndex == columns) {
    		return true;
    	}   
    	return false;
    }
    
    private void markGameAsDone(GameStatusResponse gameStatusResponse, String playerId, String gameId) {
		GameStatusResponse.Builder builder = new GameStatusResponse.Builder();
		builder = builder.fromPrototype(gameStatusResponse);
		GameStatusResponse newGameStatusResponse = builder.winner(playerId)
														.state("DONE")
														.build();
		allGames.put(gameId, newGameStatusResponse);
		
    	for (int index = 0; index < inProgressGames.getGames().size(); index++) {
    		if (inProgressGames.getGames().get(index).equals(gameId)) {
    			inProgressGames.getGames().remove(index);
    			break;
    		}
    	}		
    }
    
    private boolean isGameDrawn(GameStatusResponse gameStatusResponse) {
        int rows = gameStatusResponse.getGrid().length;
        int columns = gameStatusResponse.getGrid()[0].length;
        
    	return gameStatusResponse.getMoves().equals(rows * columns);
    }
}
