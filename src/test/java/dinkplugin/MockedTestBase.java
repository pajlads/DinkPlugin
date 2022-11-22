package dinkplugin;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public abstract class MockedTestBase {

    @BeforeEach
    protected void setUp() {
        MockitoAnnotations.initMocks(this);
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
    }

}
