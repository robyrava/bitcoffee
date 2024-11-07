package GUI;

import bitcoffee.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Stack;

public class BroadcastTransaction extends JDialog {
    private JPanel BroadcastTransactionPanel;
    private JButton btnBack;
    private JTextField secretTextField;
    private JTextField prevTxIdField;
    private JTextField prevIndexField;
    private JTextField changeAddressField;
    private JTextField btcChangeAmountField;
    private JTextField targetAddressField; 
    private JButton broadcastButton;

    public BroadcastTransaction(JFrame parent) {
        super(null, java.awt.Dialog.ModalityType.TOOLKIT_MODAL);

        //tasto home
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Close the current window
                new Dashboard(parent); // Show the initial screen
            }
        });

        broadcastButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                broadcastTransaction();
            }
        });

        setTitle("BroadcastTransaction");
        ImageIcon icon = new ImageIcon("src/GUI/images/icons8-blockchain-2.png");
        setIconImage(icon.getImage());
        setContentPane(BroadcastTransactionPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(1000, 600));
        setModal(true);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void broadcastTransaction() {
        try {
            String secretText = secretTextField.getText();
            String prevTxId = prevTxIdField.getText();
            int prevIndex = Integer.parseInt(prevIndexField.getText());
            String changeAddress = changeAddressField.getText();
            double btcChangeAmount = Double.parseDouble(btcChangeAmountField.getText());
            String targetAddress = targetAddressField.getText();

            var secretBytes = Kit.hash256(secretText);
            var mypk = new PrivateKey(secretBytes);

            var myaddress = mypk.point.getP2pkhAddress(true);
            var wif = mypk.getWIF(true, true);

            var prevTx = Kit.hexStringToByteArray(prevTxId);
            byte[] scriptNull = {};
            var txIn = new TxIn(prevTx, prevIndex, scriptNull);

            var changeAmount = (int) (btcChangeAmount * 100000000);
            var changeH160 = Kit.decodeBase58(changeAddress);
            var changeScript = new P2PKHScriptPubKey(changeH160);
            var changeOutput = new TxOut(changeAmount, changeScript.rawSerialize());

            var targetBtcAmount = 0.0001;
            var targetAmount = (int) (targetBtcAmount * 100000000);
            var targetH160 = Kit.decodeBase58(targetAddress);
            var targetScript = new P2PKHScriptPubKey(targetH160);
            var targetOutput = new TxOut(targetAmount, targetScript.rawSerialize());

            var txIns = new ArrayList<TxIn>();
            var txOuts = new ArrayList<TxOut>();
            txOuts.add(changeOutput);
            txOuts.add(targetOutput);
            txIns.add(txIn);
            var txObj = new Tx(1, txIns, txOuts, 0, true);

            var inputIndex = 0;
            var z = txObj.getSigHash(inputIndex);
            var der = mypk.signDeterminisk(z).DER();
            var sig = Kit.hexStringToByteArray(der + "01");
            var sec = Kit.hexStringToByteArray(mypk.point.SEC33());
            var cmds = new Stack<ScriptCmd>();
            cmds.push(new ScriptCmd(ScriptCmd.Type.DATA, sec));
            cmds.push(new ScriptCmd(ScriptCmd.Type.DATA, sig));
            var scriptsig = new Script(cmds);

            var txins = txObj.getTxIns();
            var newTxIn = new TxIn(txins.get(inputIndex).getPrevTxId(), txins.get(inputIndex).getPrevIndex(), scriptsig.rawSerialize());
            txins.set(inputIndex, newTxIn);
            var newTx = new Tx(txObj.getVersion(), txIns, txObj.getTxOuts(), txObj.getLocktime(), txObj.isTestnet());

            JOptionPane.showMessageDialog(this,
                    "Created tx with content:\n" + newTx + "\nFees: " + newTx.calculateFee() + "\nChecking validity: " + newTx.verify() + "\n>>>>>> PLEASE USE THIS RAW TEXT BELOW TO BROADCAST TX: \n" + newTx.getSerialString(),
                    "Transaction Created", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error creating transaction: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        BroadcastTransaction myBroadcastTransaction = new BroadcastTransaction(null);
    }
}
