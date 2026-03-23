package org.example.sdpclient.configuration;

import org.example.sdpclient.enums.MedicineType;
import org.example.sdpclient.repository.MedicineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SeedMedicine.class)
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class SeedMedicineJpaTest {

    @Autowired SeedMedicine runner;
    @Autowired MedicineRepository repository;
    @Autowired TestEntityManager em;

    @Test
    void run_insertsAll_andIsIdempotent() {
        runner.run(new DefaultApplicationArguments(new String[0]));
        em.flush();
        em.clear();

        long expected = MedicineType.values().length;
        assertThat(repository.count()).isEqualTo(expected);

        runner.run(new DefaultApplicationArguments(new String[0]));
        em.flush();
        em.clear();

        assertThat(repository.count()).isEqualTo(expected);
    }
}
