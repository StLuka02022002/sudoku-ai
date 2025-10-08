package messaging.solution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import messaging.Solution;
import messaging.TelegramInfo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionsOneInfo {

    private Solution solution;
    private Integer solutionId;
    private Integer countSolution;
    private TelegramInfo telegramInfo;
}
