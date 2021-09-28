import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TxIn {
    private final byte[] prev_tx;
    private final long prev_index;
    private final byte[] script_sig;
    private final byte[] sequence;
    private final byte[] serialized;

    public TxIn(byte[] prev_tx, long prev_index, byte[] script_sig, byte[] sequence) {
        this.prev_tx = prev_tx;
        this.prev_index = prev_index;
        this.script_sig = script_sig;
        this.sequence = sequence;

        this.serialized = this.serialize();
    }
    public TxIn(byte[] prev_tx, long prev_index, byte[] script_sig) {
        this.prev_tx = prev_tx;
        this.prev_index = prev_index;
        this.script_sig = script_sig;
        this.sequence = CryptoKit.hexStringToByteArray("ffffffff");
        this.serialized = this.serialize();
    }

    @Override
    public String toString() {
        String prev_tx_str = CryptoKit.bytesToHexString(prev_tx);
        String script_sig_str = CryptoKit.bytesToHexString(script_sig);
        String sequence = CryptoKit.bytesToHexString(this.sequence);

        return "\nTxIn{" + "prev_tx='" + prev_tx_str + '\'' + ", prev_index=" + prev_index + ", script_sig=" + script_sig_str + ", sequence=" + sequence + '}';
    }


    // parses the stream to create a TxIn instance
    public static TxIn parse(ByteArrayInputStream bis) {
        TxIn tx_input = null;
        try {
            var prev_tx = CryptoKit.reverseBytes(bis.readNBytes(32));
            String prev_tx_hex = CryptoKit.bytesToHexString(prev_tx);
            //System.out.println("TxIn parsing: found previous_tx "+prev_tx_hex);

            var prev_index = CryptoKit.litteEndianBytesToInt(bis.readNBytes(4)).longValue();
            //System.out.println("TxIn parsing: found prev_index "+prev_index);

            var script_sig_len = (int)CryptoKit.readVarint(bis);
            var script_sig = bis.readNBytes(script_sig_len);
            var script_sig_hex = CryptoKit.bytesToHexString(script_sig);
            //System.out.println("TxIn parsing: found ScriptSig "+script_sig_hex);
            var sequence = CryptoKit.reverseBytes(bis.readNBytes(4));
            tx_input = new TxIn(prev_tx,prev_index,script_sig,sequence);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return tx_input;
    }

    private byte[] serialize() {
        var bos = new ByteArrayOutputStream();

        try {
            Script script = Script.parse(this.script_sig);
            // when serializing, convert to little endian
            // 32 bytes hash of the previous tx, in little endian
            bos.write(CryptoKit.reverseBytes(prev_tx));

            byte[] buf = CryptoKit.intToLittleEndianBytes(prev_index);
            // we need only the first 4 bytes of buf
            bos.write(buf,0,4);

            bos.write(script.serialize());
            buf = CryptoKit.reverseBytes(sequence);
            // we need only the first 4 bytes of buf
            bos.write(buf,0,4);


        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public byte[] getSerialized() {
        return serialized;
    }

    public Tx fetchTx(boolean testnet) {
        return TxFetcher.fetch(CryptoKit.bytesToHexString(this.prev_tx),testnet,false);
    }

    public long getValue(boolean testnet) {
        var tx = fetchTx(testnet);
        long amount = tx.tx_outs.get((int)this.prev_index).getAmount();
        return amount;
    }

    public byte[] getScriptPubKey(boolean testnet) {
        var tx = fetchTx(testnet);
        return tx.tx_outs.get((int)this.prev_index).getScriptPubkey();
    }

    public byte[] getScript_sig() {
        return script_sig;
    }
}
