package com.bl;

import com.blservice.CustomerBLService;
import com.dao.AccountDao;
import com.dao.CustomerDao;
import com.model.Account;
import com.model.Address;
import com.model.Customer;
import com.util.MailUtil;
import com.util.UserState;
import com.vo.CustomerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.util.ResultMessage;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerBL implements CustomerBLService {

    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private AccountDao accountDao;

    //>200,level=1 ; >500,level=2 ; >1000,level=3 ; >1800,level=4 ; >2500,level=5
    private static final double[] levelLines={200,500,1000,1800,2500};

    @Override
    public ResultMessage signUpByEmail(String email, String password) {
        try {
            Customer customer = customerDao.find(email);
            if(customer == null ){//该邮箱之前未注册过
                String activeCode = MailUtil.generateCode();
                Customer newCustomer = new Customer();
//                Customer newCustomer=new Customer(email,password,"","",0,UserState.WaitToActive, activeCode,"",0);
                newCustomer.setEmail(email);
                newCustomer.setPassword(password);
                newCustomer.setState(UserState.WaitToActive);
                newCustomer.setActivecode(activeCode);
                newCustomer.setLevel(0);
                customerDao.save(newCustomer);
                new Thread( new MailUtil(email, activeCode,true)).start();
//                MailUtil.sendMail(email, activeCode);
                //发送邮件
                return ResultMessage.SUCCESS;
            }else if(customer.getState() == UserState.WaitToActive){//该邮箱注册过，但是未激活
                String activeCode = MailUtil.generateCode();
                customer.setActivecode(activeCode);
                customerDao.saveAndFlush(customer);
                new Thread( new MailUtil(email, activeCode,true)).start();
//                MailUtil.sendMail(email, activeCode);
                //再次发送邮件
                return ResultMessage.SUCCESS;
            }else{//该邮箱已经被注册过，正在使用中或者已被注销
                return ResultMessage.EXIST;
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResultMessage.FAIL;
        }

    }

    @Override
    @Transactional
    public ResultMessage activeUser(String code) {
        if(customerDao.activeUser(code)==1){
            return ResultMessage.SUCCESS;
        }
        return ResultMessage.FAIL;
    }

    @Override
    @Transactional
    public ResultMessage closeAccount(String username){
        if(customerDao.closeAccount(username)==1){
            return ResultMessage.SUCCESS;
        }
        return ResultMessage.FAIL;
    }

    @Override
    public ResultMessage firstCompleteInfo(String email, String pass, String name, String tel, String bankCard) {
        try {
            Customer customer = customerDao.find(email);
            if (customer == null) {
                return ResultMessage.FAIL;
            } else if (!customer.getPassword().equals(pass)) {
                return ResultMessage.PassError;
            } else {
                customer.setName(name);
                customer.setTelephone(tel);
                if(!(bankCard==null||bankCard.equals(""))) {
                    Account account = new Account(bankCard, getRandomBalance());
                    List<Account> accounts=new ArrayList<>();
                    accounts.add(account);
                    customer.setAccount(accounts);
                }
                customerDao.save(customer);
                return ResultMessage.SUCCESS;
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResultMessage.FAIL;
        }
    }

    @Override
    public ResultMessage customerLogin(String username, String password) {
        try {
            Customer customer = customerDao.find(username);
            if (customer == null) {
                return ResultMessage.NotExist;
            } else if (customer.getState() == UserState.CloseAccount) {
                return ResultMessage.InValid;
            } else if(customer.getState() == UserState.WaitToActive){
                return ResultMessage.ToActive;
            } else if (!password.equals(customer.getPassword())) {
                return ResultMessage.PassError;
            } else {
                return ResultMessage.CustomerLogin;
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResultMessage.FAIL;
        }
    }

    @Override
    @Transactional
    public ResultMessage findBackPass(String email) {
        try{
            String newPass = MailUtil.getRandomPassword();
            if(customerDao.resetPassword(email,newPass)==1){
                new Thread(new MailUtil(email, newPass, false)).start();
                return ResultMessage.SUCCESS;
            }else{
                return ResultMessage.NotExist;
            }
        }catch (Exception e){
            e.printStackTrace();
            return ResultMessage.FAIL;
        }
    }

    @Override
    public ResultMessage modifyPassword(String username ,String oldPassword, String newPass){
        Customer customer = customerDao.find(username);
        if(customer.getPassword().equals(oldPassword)){
            customer.setPassword(newPass);
            customerDao.saveAndFlush(customer);
            return ResultMessage.SUCCESS;
        }else{
            return ResultMessage.PassError;
        }
    }

    @Override
    @Transactional
    public void modifyName(String username, String newName) {
        customerDao.resetName(username, newName);
    }

    @Override
    @Transactional
    public void modifyTel(String username, String newTel) {
        customerDao.resetTel(username, newTel);
    }

    @Override
    public int getCustomerLevel(String username) {
        return customerDao.find(username).getLevel();
    }

    @Override
    public CustomerVO getCustomerInfo(String username){
        Customer customer=customerDao.find(username);
        return new CustomerVO(customer.getEmail(),customer.getName(),customer.getTelephone(),customer.getLevel(),customer.getAccount());
    }
    @Override
    public List<Address> getAddressList(String username) {
        return customerDao.find(username).getAddresses();
    }

    @Override
    public ResultMessage addNewAddress(String username, String province, String city, String area, String detail, String tel, String name) {
        Customer customer=customerDao.find(username);
        Address newAdd=new Address(0,province,city,area,detail,tel,name);
        if(customer.getAddresses()==null||customer.getAddresses().size()==0){
            List<Address> addresses=new ArrayList<>();
            addresses.add(newAdd);
            customer.setAddresses(addresses);
        }else{
            customer.getAddresses().add(newAdd);
        }
        customerDao.saveAndFlush(customer);
        return ResultMessage.SUCCESS;
    }

    @Override
    public ResultMessage deleteAddress(String username, List<String> address_ids) {
        Customer customer=customerDao.find(username);
        for(Address address:customer.getAddresses()){
            if(address_ids.contains(String.valueOf(address.getId()))){
                customer.getAddresses().remove(address);
                break;
            }
        }
        customerDao.saveAndFlush(customer);
        return ResultMessage.SUCCESS;
    }

    @Override
    public List<Account> getAccountList(String username) {
        return customerDao.find(username).getAccount();
    }

    @Override
    public ResultMessage addNewAccount(String username, String account) {
        Customer customer=customerDao.find(username);
        Account newAccount=new Account(account,this.getRandomBalance());
        if(customer.getAccount()==null||customer.getAccount().size()==0){
            List<Account> accounts=new ArrayList<>();
            accounts.add(newAccount);
            customer.setAccount(accounts);
        }else{
            customer.getAccount().add(newAccount);
        }
        customerDao.saveAndFlush(customer);
        return ResultMessage.SUCCESS;
    }

    @Override
    public ResultMessage deleteAccount(String username, List<String> account) {
        Customer customer=customerDao.find(username);
        for(Account a:customer.getAccount()){
            if(account.contains(a.getAccount())){
                customer.getAccount().remove(a);
                break;
            }
        }
        customerDao.saveAndFlush(customer);
        return ResultMessage.SUCCESS;
    }

    private int getRandomBalance(){
        return (int) (Math.random()*1000);
    }

    @Override
    public double getBalance(String account_id){
        return accountDao.getBalanceById(account_id);
    }

    @Override
    @Transactional
    public int accountIn(String account,double money){
        return accountDao.add(account,money);
    }

    @Override
    @Transactional
    public int accountOut(String account,double money){
        return accountDao.minus(account,money);
    }

    //levelLines={200,500,1000,1800,2500} 1,2,3,4,5
    @Override
    @Transactional
    public void updateLevel(String username,double sumConsume){
        int level=0;
        for(int i=0;i<levelLines.length;i++){
            if(sumConsume>levelLines[i]){
                level=i+1;
            }
        }
        customerDao.updateLevel(username,level);
    }
}
