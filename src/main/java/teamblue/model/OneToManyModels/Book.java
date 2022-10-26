package teamblue.model.OneToManyModels;

import lombok.EqualsAndHashCode;
import teamblue.annotations.*;

import java.time.LocalDate;

@Entity
@Table("Books")
@EqualsAndHashCode
public class Book {
    @Id
    private Long id;
    @Column(value = "Title_Of_Book")
    private String title;
    @Column("Published_at")
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

    // 2nd stage:
    @ManyToOne
    Publisher publisher = null;

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
}