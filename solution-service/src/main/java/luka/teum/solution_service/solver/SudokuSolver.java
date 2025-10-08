package luka.teum.solution_service.solver;

import messaging.Solution;

import java.util.stream.IntStream;

public class SudokuSolver {

    private static final int SUBSECTION_SIZE = 3;
    private static final int BOARD_START_INDEX = 0;
    private static final int BOARD_SIZE = Solution.SUDOKU_SIZE;
    private static final int NO_VALUE = Solution.NO_SOLUTION;
    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 9;

    public Solution solve(Solution solution) {
        int[][] answer = this.getSolve(solution.getDigits());
        return new Solution(answer);
    }

    public int[][] getSolve(int[][] digits) {
        if (this.solve(digits)) {
            return digits;
        }
        return new int[0][0];
    }

    public boolean solve(int[][] board) {
        return IntStream.range(BOARD_START_INDEX, BOARD_SIZE)
                .boxed()
                .flatMap(row -> IntStream.range(BOARD_START_INDEX, BOARD_SIZE)
                        .mapToObj(column -> new int[]{row, column}))
                .filter(position -> board[position[0]][position[1]] == NO_VALUE)
                .findFirst()
                .map(position -> this.solveCell(board, position[0], position[1]))
                .orElse(true);
    }

    private boolean solveCell(int[][] board, int row, int column) {
        return IntStream.rangeClosed(MIN_VALUE, MAX_VALUE)
                .anyMatch(value -> {
                    board[row][column] = value;
                    boolean isValid = this.isValid(board, row, column) && this.solve(board);
                    if (!isValid) {
                        board[row][column] = NO_VALUE;
                    }
                    return isValid;
                });
    }

    private boolean isValid(int[][] board, int row, int column) {
        return this.rowConstraint(board, row)
                && this.columnConstraint(board, column)
                && this.subsectionConstraint(board, row, column);
    }

    private boolean rowConstraint(int[][] board, int row) {
        boolean[] constraint = new boolean[BOARD_SIZE];
        return IntStream.range(BOARD_START_INDEX, BOARD_SIZE)
                .allMatch(column -> this.checkConstraint(board, row, constraint, column));
    }

    private boolean columnConstraint(int[][] board, int column) {
        boolean[] constraint = new boolean[BOARD_SIZE];
        return IntStream.range(BOARD_START_INDEX, BOARD_SIZE)
                .allMatch(row -> this.checkConstraint(board, row, constraint, column));
    }

    private boolean subsectionConstraint(int[][] board, int row, int column) {
        boolean[] constraint = new boolean[BOARD_SIZE];
        int subsectionRowStart = (row / SUBSECTION_SIZE) * SUBSECTION_SIZE;
        int subsectionColumnStart = (column / SUBSECTION_SIZE) * SUBSECTION_SIZE;

        return IntStream.range(subsectionRowStart, subsectionRowStart + SUBSECTION_SIZE)
                .boxed()
                .flatMap(r -> IntStream.range(subsectionColumnStart, subsectionColumnStart + SUBSECTION_SIZE)
                        .mapToObj(c -> new int[]{r, c}))
                .allMatch(cell -> this.checkConstraint(board, cell[0], constraint, cell[1]));
    }

    private boolean checkConstraint(int[][] board, int row, boolean[] constraint, int column) {
        if (board[row][column] != NO_VALUE) {
            int valueIndex = board[row][column] - 1;
            if (!constraint[valueIndex]) {
                constraint[valueIndex] = true;
            } else {
                return false;
            }
        }
        return true;
    }
}
