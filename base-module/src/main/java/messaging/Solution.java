package messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Solution {

    public static final int NO_SOLUTION = 0;
    public static final int SUDOKU_SIZE = 9;

    private int[][] digits;

    public boolean isSudokuSolution() {
        if (this.digits == null) {
            return false;
        }
        return digits.length == SUDOKU_SIZE &&
                digits[0].length == SUDOKU_SIZE;
    }

    public boolean isSolved() {
        if (digits == null) {
            return false;
        }
        if (digits.length == 0) {
            return false;
        }
        if (digits[0].length == 0) {
            return false;
        }
        for (int[] digit : digits) {
            for (int i = 0; i < digits[0].length; i++) {
                if (digit[i] <= NO_SOLUTION ||
                        digit[i] > SUDOKU_SIZE) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int[] line : digits) {
            for (int i = 0; i < line.length; i++) {
                stringBuilder.append(line[i]);
                stringBuilder.append(i == line.length - 1 ? "" : ", ");
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
