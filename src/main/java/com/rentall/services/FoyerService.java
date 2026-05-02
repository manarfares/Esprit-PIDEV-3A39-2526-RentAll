package com.rentall.services;

import com.rentall.config.DatabaseConnection;
import com.rentall.dto.FoyerListItem;
import com.rentall.util.SelectableEntityLabelSql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lit le tarif nocturne sur la table {@code logement} de {@code pidev_amine}.
 * <p>
 * Requête adaptée pour utiliser la table logement au lieu de foyer.
 */
public class FoyerService implements IFoyerService {

    private static final String SQL_PRIX_PAR_NUIT =
            "SELECT prix_par_nuit FROM logement WHERE id = ? LIMIT 1";

    private final Connection connection = DatabaseConnection.getConnection();

    @Override
    public Optional<BigDecimal> getPrixParNuitParFoyerId(int foyerId) {
        try (PreparedStatement ps = connection.prepareStatement(SQL_PRIX_PAR_NUIT)) {
            ps.setInt(1, foyerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                BigDecimal v = rs.getBigDecimal("prix_par_nuit");
                if (v == null || rs.wasNull()) {
                    return Optional.empty();
                }
                return Optional.of(v);
            }
        } catch (SQLException e) {
            System.err.println("FoyerService : " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<FoyerListItem> listerFoyersPourSelection() {
        try {
            String sql = SelectableEntityLabelSql.buildFoyerSelectSql(connection);
            if (sql == null) {
                return List.of();
            }
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<FoyerListItem> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new FoyerListItem(rs.getInt("id"), rs.getString("libelle")));
                }
                return out;
            }
        } catch (SQLException e) {
            return listerFoyersIdsSeuls();
        }
    }

    private List<FoyerListItem> listerFoyersIdsSeuls() {
        List<FoyerListItem> out = new ArrayList<>();
        String sql = "SELECT id FROM logement ORDER BY id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                out.add(new FoyerListItem(id, "Réf. logement " + id));
            }
        } catch (SQLException ignored) {
            // table absente ou autre : liste vide
        }
        return out;
    }
}
