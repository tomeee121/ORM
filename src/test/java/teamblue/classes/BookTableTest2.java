package teamblue.classes;

import teamblue.annotations.Column;
import teamblue.annotations.Entity;
import teamblue.annotations.Id;
import teamblue.annotations.Table;

import java.time.LocalDate;
@Entity
@Table()
public class BookTableTest2 {

    @Id
    private Long id;
    @Column(value = "Title_Of_Book")
    private String title;
    @Column(value = "Published_At")
    private LocalDate publishedAt;

    public BookTableTest2() {
    }

    public BookTableTest2(String title, LocalDate publishedAt) {
        this.title = title;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDate publishedAt) {
        this.publishedAt = publishedAt;
    }
}



