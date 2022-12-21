package nl.tudelft.sem.template.hoa.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.NoArgsConstructor;
import nl.tudelft.sem.template.hoa.db.ReportsConverter;


/**
 * DDD entity representing an association in our domain.
 */
@Entity
@Table(name = "Hoa")
@NoArgsConstructor
public class Hoa {

    /**
     * Identifier for an HOA in the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, unique = true)
    private long id;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "reports", nullable = false)
    @Convert(converter = ReportsConverter.class)
    private Map<String, List<Long>> reports;


    public long getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getName() {
        return name;
    }

    public Map<String, List<Long>> getReports() { return reports; }

    /**
     * Private constructor for the HOA class.
     *
     * @param country the country of the HOA
     * @param city    the city of the HOA
     * @param name    the name of the HOA
     */
    private Hoa(String country, String city, String name) {
        this.country = country;
        this.city = city;
        this.name = name;
        this.reports = new HashMap<>();
    }

    /**
     * Public method that can be called to create an HOA.
     *
     * @param country the country of the HOA
     * @param city    the city of the HOA
     * @param name    the name of the HOA
     * @return an HOA
     */
    public static Hoa createHoa(String country, String city, String name) {
        return new Hoa(country, city, name);
    }

    public void report(String memberId, Long report) {
        reports.get(memberId).add(report);
    }
    /**
     * Equals method for HOA class.
     *
     * @param o another object
     * @return true, if this is equal to the other
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Hoa hoa = (Hoa) o;
        return Objects.equals(country, hoa.country) && Objects.equals(city, hoa.city) && Objects.equals(name, hoa.name);
    }

    /**
     * Implementation of hash code for Hoa class.
     *
     * @return a hash of this.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, country, city, name);
    }
}
