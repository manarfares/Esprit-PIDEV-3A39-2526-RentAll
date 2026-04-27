package tn.piapp.dao;

import tn.piapp.db.DbConnection;
import tn.piapp.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    private static final String FIND_ALL =
            "SELECT id, name, type FROM category ORDER BY type, name";

    private static final String FIND_BY_TYPE =
            "SELECT id, name, type FROM category WHERE type = ? ORDER BY name";

    private static final String INSERT =
            "INSERT INTO category (name, type) VALUES (?, ?)";

    private static final String UPDATE =
            "UPDATE category SET name = ?, type = ? WHERE id = ?";

    private static final String DELETE =
            "DELETE FROM category WHERE id = ?";

    /** Returns all categories across all types, ordered by type then name. */
    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Returns all categories of the given type ("tool" or "service").
     * Returns an empty list (never null) if no rows match.
     */
    public List<Category> findByType(String type) throws SQLException {
        List<Category> list = new ArrayList<>();
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_TYPE)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Inserts a new category and sets the generated id on the object. */
    public void insert(Category c) throws SQLException {
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getType());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
        }
    }

    /** Updates an existing category's name and type. */
    public void update(Category c) throws SQLException {
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getType());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        }
    }

    /** Deletes a category by id. */
    public void delete(int id) throws SQLException {
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setType(rs.getString("type"));
        return c;
    }
}
