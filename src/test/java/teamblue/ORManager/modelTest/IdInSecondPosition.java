package teamblue.ORManager.modelTest;

import teamblue.annotations.Entity;
import teamblue.annotations.Id;

@Entity
public class IdInSecondPosition {
    String name;
    @Id
    Long id;
}



