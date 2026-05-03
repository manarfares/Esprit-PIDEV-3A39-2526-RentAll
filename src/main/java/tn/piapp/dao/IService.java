package tn.piapp.dao;

import java.util.List;

public interface IService<T> {
    boolean ajouter(T t);
    void modifier(T t);
    void supprimer(T t);
    List<T> recuperer();
}