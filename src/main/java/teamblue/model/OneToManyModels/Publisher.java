package teamblue.model.OneToManyModels;

import teamblue.annotations.Entity;
import teamblue.annotations.Id;
import teamblue.annotations.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Publisher {
    @Id
    private Long id;
    private String name;

    public Publisher() {
    }

    public Publisher(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // 2nd stage
    @OneToMany
    private List<Book> books = new ArrayList<>();

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }
}
