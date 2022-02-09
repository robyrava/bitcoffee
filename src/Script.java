import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

public class Script {
    final Stack<ScriptCmd> commands;

    public Script(Stack<ScriptCmd> stack) {
        this.commands = Objects.requireNonNullElseGet(stack, Stack::new);
    }
    /*************************************************************************/
    public Script(byte[] commands_bytes) {
        Script script = null;
        try {
            script =  parseSerial(Kit.addLenPrefix(commands_bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (script==null)
            this.commands = new Stack<>();
        else
            this.commands = script.commands;

    }
    /*************************************************************************/
    public static Script parseSerial(byte[] serial) throws IOException {
        Stack<ScriptCmd> ops_stack = new Stack<>();
        var bis = new ByteArrayInputStream(serial);
        var hex = Kit.bytesToHexString(serial);
        //System.out.println("DEBUG: Parsing script hex:"+hex);
        var len = Kit.readVarint(bis);

        int count =0;
        while (count < len) {
            var current_byte = bis.read();
            count++;

            // if byte is between 0x01 e 0x4b it indicates the number of bytes
            // to read the data element
            if (current_byte>=1 && current_byte <=75) {
                var cmd = new ScriptCmd(ScriptCmdType.DATA,bis.readNBytes(current_byte));
                ops_stack.push(cmd);
                //System.out.println("DEBUG: Script parsing found element data: "+cmd);
                count+= current_byte;
            }
            // OP_PUSHDATA_1 - the next byte indicate how many bytes to read
            else if (current_byte==76) {
                // TODO: why little endian over a single byte?
                var data_len = Kit.litteEndianBytesToInt(bis.readNBytes(1)).intValue();
                var cmd = new ScriptCmd(ScriptCmdType.OP_PUSHDATA1, bis.readNBytes(data_len));
                ops_stack.push(cmd);
                //System.out.println("DEBUG: Script parse operation: "+cmd);
                count+=data_len+1;
            }
            // OP_PUSHDATA_2 - the next two bytes indicate how many bytes to read for the element
            else if (current_byte==77) {
                var data_len = Kit.litteEndianBytesToInt(bis.readNBytes(2)).intValue();
                var cmd = new ScriptCmd(ScriptCmdType.OP_PUSHDATA2, bis.readNBytes(data_len));
                ops_stack.push(cmd);
                // System.out.println("DEBUG: Script parse operation: "+cmd);
                count+=data_len+2;
            }
            // OP_PUSHDATA_4 - the next four bytes indicate how many bytes to read for the element
            else if (current_byte==78) {
                var data_len = Kit.litteEndianBytesToInt(bis.readNBytes(4)).intValue();
                var cmd = new ScriptCmd(ScriptCmdType.OP_PUSHDATA4, bis.readNBytes(data_len));
                ops_stack.push(cmd);
                // System.out.println("DEBUG: Script parse operation: "+cmd);
                count+=data_len+4;
            }

            else {
                byte[] bytes = new byte[1];
                bytes[0] = (byte)current_byte;
                var cmd = new ScriptCmd(ScriptCmdType.fromInt(current_byte), bytes);
                //System.out.println("DEBUG: Script parse operation: "+cmd);
                ops_stack.push(cmd);
            }
        }
        try {
            if (count!=len)
                throw new Exception("Script parsing error: Wrong length (count="+count+",len="+len);
        } catch (Exception e) {
            e.printStackTrace();
        }

        var reversed = new Stack<ScriptCmd>();

        while (!ops_stack.empty()) {
            reversed.push(ops_stack.pop());
        }

        return new Script(reversed);
    }

    /*************************************************************************/
    // used for encoding stack nums
    public static byte[] encodeNum(BigInteger n) {
        // TOOD: check if better empty or null
        byte[] res;
        var bos = new ByteArrayOutputStream();

        // empty byte if 0
        if (n.compareTo(BigInteger.ZERO)==0)
            return null;

        var abs_n = n.abs();
        boolean negative = n.compareTo(BigInteger.ZERO) <0;

        // we revert the bytes of the number (should be little endian)
        while (abs_n.compareTo(BigInteger.ZERO)!=0) {
            var absn_bytes =abs_n.toByteArray();
            bos.write(absn_bytes[absn_bytes.length-1]);
            abs_n = abs_n.shiftRight(8);
        }

        res = bos.toByteArray();

        // we must restore the sign, if necessary....
        // if the most significant byte (rightmost, since is little endian)
        // is like 1xxxxxxx we must be sure that the 1 is not considered as sign
        if ((res[res.length-1] & 0x80)!=0) {
            // we add a further 1xxxxxxxx byte on the right, to encode the sign
            if (negative)
                bos.write(0x80);
            else
                bos.write(0);
            res = bos.toByteArray();
        } // there was not 1xxxxxxxxx, so we can just add the 1 for the required sign
        else if (negative) {
            res[res.length-1] |= 0x80;
        }
        return res;
    }

    /*************************************************************************/
    public static byte[] encodeNum(long n) {
        return encodeNum(BigInteger.valueOf(n));
    }

    /*************************************************************************/
    public static BigInteger decodeNum(byte[] element) {
        BigInteger res;
        boolean negative;
        if (element==null)
            return BigInteger.ZERO;
        var big_endian = Kit.reverseBytes(element);

        if ((big_endian[0] & 0x80) !=0 ) {
            negative = true;
            res = BigInteger.valueOf(big_endian[0] & 0x7f);
        }
        else {
            negative = false;
            res = BigInteger.valueOf((int)(big_endian[0]));
        }

        for (int i=1; i<big_endian.length;i++) {
            res = res.shiftLeft(8);
            res = res.add(BigInteger.valueOf(big_endian[i]));
        }

        if (negative)
            return res.negate();

        return res;
    }

    /***************************************************************************/
    // Convert the 20 bytes hash in a ScriptPubKey
    public static Script h160ToP2pkh(byte[] h160) {
        var cmds = new Stack<ScriptCmd>();
        cmds.push(new ScriptCmd(ScriptCmdType.OP_CHECKSIG));
        cmds.push(new ScriptCmd(ScriptCmdType.OP_EQUALVERIFY));
        cmds.push(new ScriptCmd(ScriptCmdType.DATA,h160));
        cmds.push(new ScriptCmd(ScriptCmdType.OP_HASH160));
        cmds.push(new ScriptCmd(ScriptCmdType.OP_DUP));

        return new Script(cmds);
    }
    /*************************************************************************/
    public static Script h160ToP2psh(byte[] h160) {
        var cmds = new Stack<ScriptCmd>();
        cmds.push(new ScriptCmd(ScriptCmdType.OP_EQUAL));
        cmds.push(new ScriptCmd(ScriptCmdType.DATA,h160));
        cmds.push(new ScriptCmd(ScriptCmdType.OP_HASH160));

        return new Script(cmds);
    }
    /*************************************************************************/
    public static Script h160ToP2wpkh(byte[] h160) {
        var cmds = new Stack<ScriptCmd>();
        cmds.push(new ScriptCmd(ScriptCmdType.DATA,h160));
        cmds.push(new ScriptCmd(ScriptCmdType.OP_0));

        return new Script(cmds);
    }
    /*************************************************************************/
    // check for the pattern: OP_DUP OP_HASH160 <20 byte hash> OP_EQUALVERIFY OP_CHECKSIG
    public boolean isP2pkhScriptPubKey() {
        return (this.commands.size()==5
                && commands.elementAt(0).type == ScriptCmdType.OP_CHECKSIG
                && commands.elementAt(1).type == ScriptCmdType.OP_EQUALVERIFY
                && commands.elementAt(2).type == ScriptCmdType.DATA
                && commands.elementAt(2).value.length == 20
                && commands.elementAt(3).type == ScriptCmdType.OP_HASH160
                && commands.elementAt(4).type == ScriptCmdType.OP_DUP);
    }

    /*************************************************************************/
    // check for the pattern: OP_HASH160 <20 byte hash> OP_EQUAL
    public boolean isP2shScriptPubKey() {
        return (this.commands.size()==3
                && commands.elementAt(0).type == ScriptCmdType.OP_EQUAL
                && commands.elementAt(1).type == ScriptCmdType.DATA
                && commands.elementAt(1).value.length == 20
                && commands.elementAt(2).type == ScriptCmdType.OP_HASH160);
    }
    /*************************************************************************/
    // check for the pattern: OP_0 <20 byte hash>
    public boolean isP2wpkhScriptPubKey() {
        return (this.commands.size()==2
                && commands.elementAt(0).type == ScriptCmdType.DATA
                && commands.elementAt(0).value.length == 20
                && commands.elementAt(1).type == ScriptCmdType.OP_0);
    }

    /*************************************************************************/
    // Returns the address

    public String getAddress(boolean testnet) {
        if (this.isP2pkhScriptPubKey()) {
            var h160 = commands.elementAt(2).value;
            return Kit.h160ToP2pkhAddress(h160,testnet);
        }
        else if (this.isP2shScriptPubKey())
        {
            var h160= commands.elementAt(1).value;
            return Kit.h160ToP2shAddress(h160,testnet);
        }
        throw new RuntimeException("Wrong invokation of getAddress on non-scriptpubkey");
    }



    /*************************************************************************/
    public void addTop(Stack<ScriptCmd> other) {
        this.commands.addAll(other);
    }

    public void addTop(Script other_script) {
        this.commands.addAll(other_script.commands);
    }
    /*************************************************************************/
    // bytes encoding ops, without length prefix (required in serialization)
    public byte[] getBytes() {
        try {
            return this.raw_serialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /*************************************************************************/
    private byte[] raw_serialize() throws IOException {
        var bos = new ByteArrayOutputStream();

        var copy_cmd = new Stack<ScriptCmd>();

        copy_cmd.addAll(commands);

        if (copy_cmd.empty()) {
            return new byte[]{};
        }


        while (!copy_cmd.empty()) {
            var cmd = copy_cmd.pop();
            var len = cmd.value.length;

            if (cmd.type== ScriptCmdType.DATA) {
                bos.write((byte)len);
                bos.write(cmd.value);
            }
            else if (cmd.type== ScriptCmdType.OP_PUSHDATA1) {
                bos.write((byte) ScriptCmdType.OP_PUSHDATA1.getValue());
                bos.write((byte)len);
                bos.write(cmd.value);
            }
            else if (cmd.type== ScriptCmdType.OP_PUSHDATA2) {
                bos.write((byte) ScriptCmdType.OP_PUSHDATA2.getValue());
                var len_bytes = Kit.intToLittleEndianBytes(len);
                bos.write(len_bytes,0,2);
                bos.write(cmd.value);
            } // operation, not data
            else {
                bos.write((byte)cmd.type.getValue());
                //bos.write(cmd.value);
            }

        }
        return bos.toByteArray();
    }

    /*************************************************************************/
    public byte[] serialize() {
        var bos = new ByteArrayOutputStream();
        try {
            var result = this.raw_serialize();
            var len = result.length;
            var len_bytes = Kit.encodeVarint(len);
            // serialization starts with the number script bytes that follows
            assert len_bytes != null;
            bos.write(len_bytes);
            bos.write(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
    }


    public boolean evaluate(byte[] z) {
        return this.evaluate(z,null);
    }

    /*************************************************************************/
    public boolean evaluate(byte[] z, ArrayList<byte[]> witness) {
        var cmds = new Stack<ScriptCmd>();
        cmds.addAll(this.commands); // make a copy for adding redeem script if required

        var stack = new Stack<byte[]>();
        var altstack = new Stack<byte[]>();

        System.out.println("*********************************************************************");
        System.out.println("SCRIPT-> Starting evalutation of script:");
        System.out.println(this);

        while (cmds.size()>0) {
            var cmd = cmds.pop();

            // if it is data, just move it to the stack

            if (cmd.type== ScriptCmdType.DATA) {
                stack.push(cmd.value);

                //detect p2sh pattern ////////////////////////////////////////////
                // OP_HASH160 <20bytes hash> OP_EQUAL
                if (cmds.size()==3
                        && cmds.elementAt(0).type==ScriptCmdType.OP_EQUAL
                        && cmds.elementAt(1).type==ScriptCmdType.DATA
                        && cmds.elementAt(1).value.length==20
                        && cmds.elementAt(2).type==ScriptCmdType.OP_HASH160)
                {
                    cmds.pop(); // we already know it's op_hash160
                    var h160 = cmds.pop(); // the hash value
                    cmds.pop(); // we already know it's op equal

                    if (!Op.OP_HASH160(stack)) return false;
                    stack.push(h160.value);
                    if (!Op.OP_EQUAL(stack)) return false;
                    if (!Op.OP_VERIFY(stack)) {
                        System.out.println("******************* WARNING: bad p2sh h160");
                        return false;
                    }

                    var bos = new ByteArrayOutputStream();
                    try {
                        bos.write(Objects.requireNonNull(Kit.encodeVarint(cmd.value.length)));
                        bos.write(cmd.value);
                        var redeem_script = Script.parseSerial(bos.toByteArray());
                        cmds.addAll(redeem_script.commands);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } // end p2sh /////////////////////////////////////////////////

                // p2wpks
                // nodes supporting segwit will detect this condition in the stack, as a consequence
                // of the execution of this particular script:
                // OP_0 <20bytes hash>
                // Please notice that OP_0 places an empty element in the stack, NOT a byte 0
                if (stack.size()==2 && stack.elementAt(1)==null && stack.elementAt(0).length==20) {

                    stack.pop(); // the empty byte[]
                    var h160 = stack.pop();

                    var script = Script.h160ToP2pkh(h160);
                    cmds.addAll(script.commands);
                    for (byte[] item: witness) {
                        if (item.length>0)
                            cmds.add(new ScriptCmd(ScriptCmdType.DATA,item));
                    }
                }

            }
            else { // not a data element, we must execute the opcode logic

                if (cmd.type == ScriptCmdType.OP_IF || cmd.type == ScriptCmdType.OP_NOTIF ) {
                    System.out.println("ERROR: OP_IF not implemented!");
                    System.exit(-1);

                    // require manipulation of commands using the top of stack
                    assert false;
                }
                else if (cmd.type.getValue() >= 172 && cmd.type.getValue() <= 175){
                    // require signature (CHECKSIG etc..)
                    switch (cmd.type) {
                        case OP_CHECKSIG:
                            Op.OP_CHECKSIG(stack,z);
                            break;
                        case OP_CHECKMULTISIG:
                            Op.OP_CHECKMULTISIG(stack,z);
                            break;
                        default:
                            assert false;
                    }
                }
                else {

                    switch (cmd.type) {
                        case OP_0:
                            Op.OP_0(stack);
                            break;
                        case OP_1:
                            Op.OP_1(stack);
                            break;
                        case OP_2:
                            Op.OP_2(stack);
                            break;
                        case OP_3:
                            Op.OP_3(stack);
                            break;
                        case OP_4:
                            Op.OP_4(stack);
                            break;
                        case OP_5:
                            Op.OP_5(stack);
                            break;
                        case OP_6:
                            Op.OP_6(stack);
                            break;
                        case OP_7:
                            Op.OP_7(stack);
                            break;
                        case OP_8:
                            Op.OP_8(stack);
                            break;
                        case OP_9:
                            Op.OP_9(stack);
                            break;
                        case OP_10:
                            Op.OP_10(stack);
                            break;
                        case OP_11:
                            Op.OP_11(stack);
                            break;
                        case OP_12:
                            Op.OP_12(stack);
                            break;
                        case OP_13:
                            Op.OP_13(stack);
                            break;
                        case OP_14:
                            Op.OP_14(stack);
                            break;
                        case OP_15:
                            Op.OP_15(stack);
                            break;
                        case OP_16:
                            Op.OP_16(stack);
                            break;

                        case OP_VERIFY:
                            Op.OP_VERIFY(stack);
                            break;

                        case OP_RETURN:
                            Op.OP_RETURN(stack);
                            break;
                        case OP_TOALTSTACK:
                            Op.OP_TOALTSTACK(stack,altstack);
                            break;
                        case OP_FROMALTSTACK:
                            Op.OP_FROMALTSTACK(stack,altstack);
                            break;
                        case OP_2DUP:
                            Op.OP_2DUP(stack);
                            break;
                        case OP_EQUAL:
                            Op.OP_EQUAL(stack);
                            break;
                        case OP_EQUALVERIFY:
                            Op.OP_EQUALVERIFY(stack);
                            break;
                        case OP_NOT:
                            Op.OP_NOT(stack);
                            break;
                        case OP_ADD:
                            Op.OP_ADD(stack);
                            break;
                        case OP_SUB:
                            Op.OP_SUB(stack);
                            break;
                        case OP_MUL:
                            Op.OP_MUL(stack);
                            break;
                        case OP_RIPEMD160:
                            Op.OP_RIPEMD160(stack);
                            break;
                        case OP_SHA256:
                            Op.OP_SHA256(stack);
                            break;
                        case OP_DUP:
                            Op.OP_DUP(stack);
                            break;
                        case OP_HASH256:
                            Op.OP_HASH256(stack);
                            break;

                        case OP_HASH160:
                            Op.OP_HASH160(stack);
                            break;
                        default:
                            System.out.println("FATAL: unsupported Script command "+cmd);
                            System.exit(-1);
                    }
                    // require only stack
                }
            }

        }
        if (stack.size()==0) return false;
        return stack.pop() != null;
    }


    /*************************************************************************/
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("Script Stack:");

        for (int i=this.commands.size()-1; i>-1; i--) {
            out.append(this.commands.get(i));
        }
        return out.toString();

    }
}
