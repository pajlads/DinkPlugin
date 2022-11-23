package dinkplugin;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public abstract class MockedTestBase {

    private AutoCloseable mocks;

    @BeforeEach
    protected void setUp() {
        this.mocks = MockitoAnnotations.openMocks(this);
        Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
    }

    @AfterEach
    protected void cleanUp() throws Exception {
        mocks.close();
    }

}
