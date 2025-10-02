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
public class SolutionsInfo {

    private List<Solution> solutions;
    private Integer countSolutions;
    private TelegramInfo telegramInfo;
}
