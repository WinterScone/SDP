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
@Import(Medicines16Rows.class)
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class Medicines16RowsJpaTest {

    @Autowired Medicines16Rows runner;
    @Autowired MedicineRepository repository;
    @Autowired org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager em;

    @Test
    void run_insertsAll_andIsIdempotent() {
        runner.run(new DefaultApplicationArguments(new String[0]));
        em.flush(); // <-- will throw with the real root cause
        em.clear();

        long expected = MedicineType.values().length;
        assertThat(repository.count()).isEqualTo(expected);

        runner.run(new DefaultApplicationArguments(new String[0]));
        em.flush();
        em.clear();

        assertThat(repository.count()).isEqualTo(expected);
    }
}

