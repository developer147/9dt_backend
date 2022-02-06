package com._98point6.droptoken;

import java.util.ArrayList;
import java.util.HashMap;
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
        
        return Response.ok(gameStatusResponse).build();
    }

    @Path("/{id}/{playerId}")
    @POST
    public Response postMove(@PathParam("id")String gameId, @PathParam("playerId") String playerId, PostMoveRequest request) {
        logger.info("gameId={}, playerId={}, move={}", gameId, playerId, request);
        return Response.ok(new PostMoveResponse()).build();
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
        return Response.ok(new GetMovesResponse()).build();
    }

    @Path("/{id}/moves/{moveId}")
    @GET
    public Response getMove(@PathParam("id") String gameId, @PathParam("moveId") Integer moveId) {
        logger.info("gameId={}, moveId={}", gameId, moveId);
        return Response.ok(new GetMoveResponse()).build();
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
									  .moves(0)
									  .winner(null)
									  .state("IN_PROGRESS").build(); //better could be an enum
        allGames.put(newGame, gameStatusResponse);    	
    }

}
