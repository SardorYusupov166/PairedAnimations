package dev.sardoryusupov.pairedanimations;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChatPairProtocolTest {
    @Test void parsesRealMigosMcPrivateMessages() {
        var p = ChatPairProtocol.parse("[~MLPGOD -> я] PA1:I:abcdef012345", "OtherUser");
        assertNotNull(p);
        assertEquals("MLPGOD", p.senderName());
        assertEquals('I', p.operation());
        assertEquals("abcdef012345", p.nonce());
    }

    @Test void matchesArrowAndClanPrefix() {
        var p = ChatPairProtocol.parse("[VIP ~NoXy → я] PA1:A:123456789abc", "Friend");
        assertNotNull(p);
        assertEquals("NoXy", p.senderName());
    }

    @Test void ignoresOwnEchoAndOrdinaryChat() {
        assertNull(ChatPairProtocol.parse("[я -> ~MLPGOD] PA1:I:abcdef012345", "Player"));
        assertNull(ChatPairProtocol.parse("<MLPGOD> PA1:I:abcdef012345", "Player"));
        assertNull(ChatPairProtocol.parse("[~MLPGOD -> я] 123", "Player"));
        assertNull(ChatPairProtocol.parse("[~MLPGOD -> я] PA1:I:1234", "Player"));
        assertNull(ChatPairProtocol.parse("[~MLPGOD -> другая] PA1:I:abcdef012345", "Player"));
    }

    @Test void startContainsTimestamp() {
        var p = ChatPairProtocol.parse(
            "[~Roksi -> я] PA1:S:abcdef012345:1793820000000", "Player");
        assertNotNull(p);
        assertEquals('S', p.operation());
        assertEquals(1793820000000L, p.startEpochMillis());
    }

    @Test void roundTripsOperations() {
        for (char op : new char[] {'I','A','D','C'}) {
            String encoded = ChatPairProtocol.encode(op, "deadbeef1234", 0);
            var p = ChatPairProtocol.parse("[~ghost_07 -> я] " + encoded, "mlpgod");
            assertNotNull(p);
            assertEquals(op, p.operation());
        }
        assertThrows(IllegalArgumentException.class,
            () -> ChatPairProtocol.encode('I', "x", 0));
    }

    @Test void rejectsBrokenStartAndDifferentNames() {
        assertNull(ChatPairProtocol.parse(
            "[~Roksi -> я] PA1:S:abcdef012345:zzz", "Player"));
        assertNull(ChatPairProtocol.parse(
            "[~Roksi -> я] PA1:S:abcdef012345", "Player"));
        assertNull(ChatPairProtocol.parse(
            "[я -> ~Roksi] PA1:S:abcdef012345:1793820000000", "Player"));
    }
}
