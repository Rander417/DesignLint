package designlint.fixtures;

import java.util.Optional;

/**
 * Test fixture: Optional used only as a return type. Should pass.
 * This is the intended usage per Java architects.
 */
public class OptionalReturnOnly {
    private String nickname;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Optional<String> getNickname() {
        return Optional.ofNullable(nickname);
    }
}
