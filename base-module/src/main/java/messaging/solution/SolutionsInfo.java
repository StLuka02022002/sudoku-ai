package messaging.solution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import messaging.Solution;
import messaging.TelegramInfo;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionsInfo {

    private Set<Solution> solutions;
    private Integer countSolutions;
    private TelegramInfo telegramInfo;
}
