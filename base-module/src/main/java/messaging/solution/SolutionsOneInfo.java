package messaging.solution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import messaging.TelegramInfo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionsOneInfo {

    private SolutionsInfo solutionsInfo;
    private Integer solutionId;
    private Integer countSolution;
    private TelegramInfo telegramInfo;
}
