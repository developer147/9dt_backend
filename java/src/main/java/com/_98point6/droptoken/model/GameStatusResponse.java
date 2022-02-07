package com._98point6.droptoken.model;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class GameStatusResponse {
    private List<String> players;
    //private Integer moves;
    private String winner;
    private String state;
    
    // below are added by me
    private Integer columns;
    private Integer rows;
    // Pointer to keep track of which player's turn to play next
    private Integer turn;
    private Integer moves;
    GetMovesResponse movesResponse;

    public GameStatusResponse() {}

    private GameStatusResponse(Builder builder) {
        this.players = Preconditions.checkNotNull(builder.players);
        //this.moves = Preconditions.checkNotNull(builder.moves);
        this.winner = builder.winner;
        this.state = Preconditions.checkNotNull(builder.state);
        this.columns = Preconditions.checkNotNull(builder.columns);
        this.rows = Preconditions.checkNotNull(builder.rows);
        this.turn = 0; //by default it's always the first player's turn to start game.
        this.moves = 0;
        
        GetMovesResponse.Builder getMovesResponseBuilder = new GetMovesResponse.Builder();
        this.movesResponse = getMovesResponseBuilder.moves(new ArrayList<GetMoveResponse>())
        									   .build();
    }

    public List<String> getPlayers() {
        return players;
    }

//    public Integer getMoves() {
//        return moves;
//    }

    public Optional<String> getWinner() {
        return Optional.ofNullable(winner);
    }

    public String getState() {
        return state;
    }
    
    public Integer getColumns() {
    	return columns;
    }
    
    public Integer getRows() {
    	return rows;
    }
    
    public Integer getTurn() {
    	return turn;
    }
    
    public void setTurn() {
    	if (turn == (players.size() - 1)) {
    		turn = 0;
    	} else {
    		turn++;
    	}
    }
    
    public Integer getMoves() {
    	return moves;
    }
    
    public void incrementMoves() {
    	moves = moves + 1;
    }
    
    public GetMovesResponse getMovesResponse() {
    	return movesResponse;
    }

    public static class Builder {
        private List<String> players;
        //private Integer moves;
        private String winner;
        private String state;
        private Integer columns;
        private Integer rows;

        public Builder players(List<String> players) {
            this.players = players;
            return this;
        }

//        public Builder moves(Integer moves) {
//            this.moves = moves;
//            return this;
//        }

        public Builder winner(String winner) {
            this.winner = winner;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }
        
        public Builder columns(Integer columns) {
        	this.columns = columns;
        	return this;
        }
        
        public Builder rows(Integer rows) {
        	this.rows = rows;
        	return this;
        }

        public Builder fromPrototype(GameStatusResponse prototype) {
            players = prototype.players;
            //moves = prototype.moves;
            winner = prototype.winner;
            state = prototype.state;
            columns = prototype.columns;
            rows = prototype.rows;
            return this;
        }

        public GameStatusResponse build() {
            return new GameStatusResponse(this);
        }
    }
}
