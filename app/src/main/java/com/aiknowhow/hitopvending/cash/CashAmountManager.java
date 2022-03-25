package com.aiknowhow.hitopvending.cash;

import com.lomoment.pmod.DevicesInitListener;
import com.lomoment.pmod.IDevices;
import com.lomoment.pmod.cash.CashDevicesManager;
import com.lomoment.pmod.cash.CashEquipmentMoneyInfo;
import com.lomoment.pmod.cash.TransactionListener;
import com.lomoment.pmod.cash.paymentstrategy.ClearBalanceSellingStrategy;
import com.lomoment.pmod.cash.paymentstrategy.FullRefundSellingStrategy;
import com.lomoment.pmod.cash.paymentstrategy.KeepBalanceSellingStrategy;
import com.lomoment.pmod.cash.paymentstrategy.StrictReceivedSellingStrategy;
import com.lomoment.pmod.mdb.MDBDevices;

import java.util.HashMap;
import java.util.Map;

/**
 * @author libin
 * @date 2020/2/27
 * @Description 现金金额管理
 */
public class CashAmountManager implements DevicesInitListener {
    public static final int PAYMENT_STRATEGY_STRICT_RECEIVED = 1;
    public static final int PAYMENT_STRATEGY_CLEAR_BALANCE = 2;
    public static final int PAYMENT_STRATEGY_KEEP_BALANCE = 3;
    public static final int PAYMENT_STRATEGY_FULL_REFUND = 4;

    public static CashAmountManager mInstance;
    private CashDevicesManager accountant;
    private MDBDevices mdbDevices;

    private DevicesInitListener initListenerList;

    private CashAmountManager() {
        accountant = new CashDevicesManager();
        accountant.setDevicesInitListener(this);
    }

    public static CashAmountManager getInstance() {
        if (mInstance == null) {
            synchronized (CashAmountManager.class) {
                if (mInstance == null) {
                    mInstance = new CashAmountManager();
                }
            }
        }
        return mInstance;
    }

    public void init(String path, int baudrate) {
        //Create an MDB object
        mdbDevices = MDBDevices.getInstance();
        //Set initialization parameters
        mdbDevices.setInitParse(path, baudrate);
        //The range of receiving coins
        mdbDevices.setCoinReceivableAmountType(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        //The range of banknotes received
        mdbDevices.setBillReceivableAmountType(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00});
        accountant.addDevices(mdbDevices);
        //Setting up the Collection Policy
        accountant.setSellingStrategy(new ClearBalanceSellingStrategy());
        //Initialize it
        initDevices();
    }

    public void initDevices() {
        if (accountant.isInitDevices()) {
            accountant.checkConnected();
        } else {
            accountant.initDevices();
        }
    }

    public boolean isInitFinish() {
        return accountant.isInitDevices();
    }


    @Override
    public void initSuccess(IDevices s) {
        if (initListenerList != null) {
            initListenerList.initSuccess(s);
        }
    }

    @Override
    public void initFinish(IDevices iDevices) {
        if (initListenerList != null) {
            initListenerList.initFinish(iDevices);
        }
    }

    @Override
    public void initFail(IDevices iDevices) {
        if (initListenerList != null) {
            initListenerList.initFail(iDevices);
        }
    }

    public void setDeviceInitListenerList(DevicesInitListener initListenerList) {
        this.initListenerList = initListenerList;
    }

    public void addTransactionListener(TransactionListener transactionListener) {
        accountant.addTransactionListener(transactionListener);
    }

    public void delTransactionListener(TransactionListener transactionListener) {
        accountant.delTransactionListener(transactionListener);
    }

    public void checkConnected() {
        accountant.checkConnected();
    }


    public void checkReceivedMoneyFinish() {
        accountant.checkReceivedMoneyFinish();
    }

    public void release() {
        if (accountant != null) {
            accountant.releaseAllDevices();
        }
    }

    public double getCustomerMoney() {
        if (accountant != null) {
            return accountant.getUserAmount();
        }
        return 0;
    }

    public IDevices getDevicesByName(String s) {
        return accountant.getDevicesByName(s);
    }

    public void setPrice(double i) {
        accountant.setPrice(i);
    }

    /**
     * Try to open the collection
     *
     * @return
     */
    public boolean tryOpenReceived() {
        return accountant.tryOpenReceived();
    }

    /**
     * To perform a refund
     */
    public void executeRefund() {
        accountant.executeRefund();
    }

    /**
     * Called when the entire transaction is complete
     */
    public void transactionFinish() {
        accountant.transactionFinish();
    }

    /**
     * Perform deductions for all customer amounts
     */
    public void executeAmountOfDeducted() {
        accountant.executeAmountOfDeducted();
    }

    /**
     * Perform a deduction of the amount specified by the customer
     *
     * @param i
     */
    public void executeAmountOfDeducted(double i) {
        accountant.executeAmountOfDeducted(i);
    }

    /**
     * Obtain cash box information
     *
     * @return
     */
    public HashMap<String, CashEquipmentMoneyInfo> getCashMoneyInfo() {
        return accountant.getCashMoneyInfo();
    }

    public Map<Double, Integer> getTubeInfo(){
        Map<Double, Integer> money = mdbDevices.getCc6100DevicesManager().getTubeInfo();
        for(Double key : money.keySet()){
        }
        return mdbDevices.getCc6100DevicesManager().getTubeInfo();
    }

    public void changeCashPaymentStrategy(int strategy) {
        if (strategy == PAYMENT_STRATEGY_STRICT_RECEIVED) {
            accountant.setSellingStrategy(new StrictReceivedSellingStrategy());
        } else if (strategy == PAYMENT_STRATEGY_CLEAR_BALANCE) {
            accountant.setSellingStrategy(new ClearBalanceSellingStrategy());
        } else if (strategy == PAYMENT_STRATEGY_KEEP_BALANCE) {
            accountant.setSellingStrategy(new KeepBalanceSellingStrategy());
        } else if (strategy == PAYMENT_STRATEGY_FULL_REFUND) {
            accountant.setSellingStrategy(new FullRefundSellingStrategy());
        }
    }


    public boolean isBusy() {
        return accountant.isBusy();
    }

    public void setUserAmount(double i) {
        accountant.setUserAmount(i);
    }

    public void increasedUserAmount(double v) {
        accountant.increasedUserAmount(v);
    }

    public void refundMoney(double v) {
        accountant.refundMoney(v);
    }

    public double getPrice() {
        return accountant.getPrice();
    }

    public boolean isCanRefundFinish(double v) {
        return accountant.isCanRefundFinish(v);
    }

    public void stopReceivedMoney() {
        accountant.stopReceivedMoney();
    }


    public void setBusy(boolean b) {
        accountant.setBusy(b);
    }

}
