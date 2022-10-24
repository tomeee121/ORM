package teamblue.ORManager.modelTest;

import teamblue.annotations.Entity;
import teamblue.annotations.Id;

@Entity
public class WithOneIdField {
    @Id
    Long id;
}