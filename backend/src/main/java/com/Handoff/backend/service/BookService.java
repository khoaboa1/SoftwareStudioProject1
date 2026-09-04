package com.Handoff.service;

import com.Handoff.model.Book;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class BookService {

    public List<Book> getAllBooks() {
        return Arrays.asList(
                new Book(1L, "The Great Gatsby", "F. Scott Fitzgerald"),
                new Book(2L, "To Kill a Mockingbird", "Harper Lee"),
                new Book(3L, "1984", "George Orwell"));
    }
}
