package designlint.fixtures;

/**
 * Test fixture: overrides finalize(). Should trigger a violation.
 * Don't do this in real code. Just... don't.
 */
@SuppressWarnings("deprecation")
public class HasFinalize {
    private Object resource;

    @Override
    protected void finalize() throws Throwable {
        try {
            resource = null; // "cleanup"
        } finally {
            super.finalize();
        }
    }
}
