package teamblue.model;

import teamblue.annotations.Entity;
import teamblue.annotations.PrimaryKey;

import java.time.LocalDate;

@Entity
public class Book {
    @PrimaryKey
    private Long id;
    private String title;
    private LocalDate publishedAt;

    public Book() {
    }

    public Book(String title, LocalDate publishedAt) {
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
