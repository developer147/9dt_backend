package com._98point6.droptoken.model;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;

/**
 *
 */
public class GameStatusResponse {
    private List<String> players;
    private Integer moves;
    private String winner;
    private String state;
    
    // below are added by me
    // Pointer to keep track of which player's turn to play next
    private String whoseTurn;
    private GetMovesResponse movesResponse;
    private String[][] grid;
    

    public GameStatusResponse() {}

    private GameStatusResponse(Builder builder) {
        this.players = Preconditions.checkNotNull(builder.players);
        this.moves = Preconditions.checkNotNull(builder.moves);
        this.winner = builder.winner;
        this.state = Preconditions.checkNotNull(builder.state);
        this.grid = Preconditions.checkNotNull(builder.grid);
        this.whoseTurn = Preconditions.checkNotNull(builder.whoseTurn);
        this.movesResponse = Preconditions.checkNotNull(builder.movesResponse);
    }

    public List<String> getPlayers() {
        return players;
    }

    public Integer getMoves() {
        return moves;
    }

    public Optional<String> getWinner() {
        return Optional.ofNullable(winner);
    }

    public String getState() {
        return state;
    }
    
    public String[][] getGrid() {
    	return grid;
    }
    
    public String getWhoseTurn() {
    	return whoseTurn;
    }
    
    public void setWhoseTurn() {
//    	if (turn == (players.size() - 1)) {
//    		turn = 0;
//    	} else {
//    		turn++;
//    	}
    	for (int index = 0; index < players.size(); index++) {
    		if (players.get(index).equals(whoseTurn)) {
    			if ((index + 1) < players.size()) {
    				whoseTurn = players.get(index + 1);
    			} else {
    				whoseTurn = players.get(0);
    			}
    			break;
    		}
    	}
    }
    
    public void incrementMoves() {
    	moves = moves + 1;
    }
    
    public GetMovesResponse getMovesResponse() {
    	return movesResponse;
    }
    
    public String getGridCell(int row, int column) {
    	return grid[row][column];
    }
    
    public void setGridCell(int row, int column, String playerId) {
    	grid[row][column] = playerId;
    }

    public static class Builder {
        private List<String> players;
        private Integer moves;
        private String winner;
        private String state;
        private GetMovesResponse movesResponse;
        private String[][] grid;
        private String whoseTurn;

        public Builder players(List<String> players) {
            this.players = players;
            return this;
        }

        public Builder moves(Integer moves) {
            this.moves = moves;
            return this;
        }

        public Builder winner(String winner) {
            this.winner = winner;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }
        
        public Builder movesResponse(GetMovesResponse movesResponse) {
        	this.movesResponse = movesResponse;
        	return this;
        }
        
        public Builder grid(String[][] grid) {
        	this.grid = grid;
        	return this;
        }
        
        public Builder whoseTurn(String whoseTurn) {
        	this.whoseTurn = whoseTurn;
        	return this;
        }

        public Builder fromPrototype(GameStatusResponse prototype) {
            players = prototype.players;
            moves = prototype.moves;
            winner = prototype.winner;
            state = prototype.state;
            movesResponse = prototype.movesResponse;
            grid = prototype.grid;
            whoseTurn = prototype.whoseTurn;
            return this;
        }

        public GameStatusResponse build() {
            return new GameStatusResponse(this);
        }
    }
}
