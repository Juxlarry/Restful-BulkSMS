package com.swiftpot.swiftalertmain.services;

import com.google.gson.Gson;
import com.swiftpot.swiftalertmain.db.model.*;
import com.swiftpot.swiftalertmain.helpers.CustomDateFormat;
import com.swiftpot.swiftalertmain.helpers.DeliveryStatus;
import com.swiftpot.swiftalertmain.ifaces.SMSSender;
import com.swiftpot.swiftalertmain.models.SMSSenderRequest;
import com.swiftpot.swiftalertmain.repositories.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Ace Programmer Rbk
 *         <Rodney Kwabena Boachie at [rodney@swiftpot.com,rbk.unlimited@gmail.com]> on
 *         02-Oct-16 @ 10:45 PM
 */
@Service
public class BaseMessageSender {

    @Autowired
    SMSSender smsSender;
    @Autowired
    MessagesReportDocRepository messagesReportDocRepository;
    @Autowired
    MessagesDetailedReportDocRepository messagesDetailedReportDocRepository;
    @Autowired
    MessageContentDocRepository messageContentDocRepository;
    @Autowired
    UserDocRepository userDocRepository;
    @Autowired
    GroupsDocRepository groupsDocRepository;


    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     *
     * @param groupContactsDocsList @NotNull
     * @param message @NotNull
     * @param senderId @NotNull
     * @return
     */
    public boolean didEachMessageSendWithoutAnyError(List<GroupContactsDoc> groupContactsDocsList,String message, String senderId,int creditBefore,String userName) {

        boolean wereAllMessagesSent;
        Gson gson = new Gson();
        String userNameGlobal = userName;
        // Set message which will be used for all contacts in group
        String messageToSendGlobal = message;
        // Set SenderId to be used for all contacts in group
        String senderIdGlobal = senderId;
        // Set MessageId to be used for all contacts in group wether it was sent or not to help in reporting to clients
        String messageIdGlobal = UUID.randomUUID().toString().toUpperCase().substring(0, 36);
        log.info("Message = "+messageToSendGlobal+" SenderId = "+senderIdGlobal+" MessageIdGlobal ="+messageIdGlobal);
        /**
         *Number of unsuccessful messages,this will be used to add back to customer's credit since it
         * has been deducted already before list is processed here
         */
        ArrayList<Boolean> unsuccessfulMessagesCount = new ArrayList<>(0);
        ArrayList<Boolean> successfulMessagesCount = new ArrayList<>(0);
        /**
         * find creditBalance before transaction
         */
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CustomDateFormat.getDateFormat());
        String dateBulkMessageRequestCame = simpleDateFormat.format(Date.from(Instant.now()));
        log.info("Credit Before ="+creditBefore);

        for (GroupContactsDoc singleContact : groupContactsDocsList) {
            log.info("\n\n==========CONTACT (" + groupContactsDocsList.indexOf(singleContact)
                    + ") DETAILS AND MESSAGE==========\n:\t");
            log.info("{}**message={}", gson.toJson(singleContact), messageToSendGlobal);
            log.info("\n\n==========CONTACT (" + groupContactsDocsList.indexOf(singleContact)
                    + ") END===========================\n\n");
            /**
             * remember only the PhoneNumber of the contact is special,
             * message and senderId are same for all contacts in the group
             */
            String contactRecipientPhoneNum = singleContact.getContactPhoneNum();
            SMSSenderRequest smsSenderRequest = new SMSSenderRequest(senderIdGlobal,messageToSendGlobal,contactRecipientPhoneNum);
            String groupId = singleContact.getGroupId();
            //SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CustomDateFormat.getDateFormat());
            String dateNow = simpleDateFormat.format(Date.from(Instant.now()));

            MessagesDetailedReportDoc messagesDetailedReportDoc= new MessagesDetailedReportDoc(dateNow,
                    messageIdGlobal,
                    contactRecipientPhoneNum,
                    senderIdGlobal,
                    groupId,
                    userNameGlobal);
            MessageContentsDoc messageContentsDoc = new MessageContentsDoc(messageIdGlobal,messageToSendGlobal,dateNow);

            if(!smsSender.isMessageSendingSuccessful(smsSenderRequest)){
                unsuccessfulMessagesCount.add(false);
                /**
                 * Set Delivery Status to NO,since message was not able to be sent
                 */
                messagesDetailedReportDoc.setDeliveryStatus(DeliveryStatus.NO.toString());
                messagesDetailedReportDocRepository.save(messagesDetailedReportDoc);
                messageContentDocRepository.save(messageContentsDoc);
            }else{
                /**
                 * Set Delivery Status to YES,since message was sent successfully
                 */
                successfulMessagesCount.add(true);
                messagesDetailedReportDoc.setDeliveryStatus(DeliveryStatus.YES.toString());
                messagesDetailedReportDocRepository.save(messagesDetailedReportDoc);
                messageContentDocRepository.save(messageContentsDoc);
            }

        }

        if(!unsuccessfulMessagesCount.isEmpty()){
            int numberOfCreditBalanceToReturnToCustomer = unsuccessfulMessagesCount.size();

            returnCreditBalanceToCustomer(numberOfCreditBalanceToReturnToCustomer, userNameGlobal);


            wereAllMessagesSent = false;
        }else{
            wereAllMessagesSent = true;
        }

        /**
         *
         * Finally save A MessageReportDoc with number of messages,credit before and after and others after saving
         * each message sent into the MessageReportDocDetailed Document
         *
         */
        int creditAfter = creditBefore - successfulMessagesCount.size();
        MessagesReportDoc messagesReportDoc = new MessagesReportDoc();
        String groupIdTemporary = groupContactsDocsList.get(0).getGroupId();
        /**
         * set groupName to Empty if finding by GroupId fails,since we are passing empty groupId for a single message,groupId can be empty in some cases
         */
        String groupName;
        try {
            groupName = StringUtils.defaultIfEmpty(groupsDocRepository.findByGroupId(groupIdTemporary).getGroupName(), "");
        }catch(NullPointerException e){
            log.info("groupId not present,hence,set empty groupName in Nullpointer exception");
            groupName ="";
        }

        int totalNumOfMessagesTried = groupContactsDocsList.size();

        messagesReportDoc.setUserName(userNameGlobal);
        messagesReportDoc.setDateCreated(dateBulkMessageRequestCame);
        messagesReportDoc.setMessageId(messageIdGlobal);
        messagesReportDoc.setGroupName(groupName);
        messagesReportDoc.setGroupId(groupIdTemporary);
        messagesReportDoc.setNoOfMessages(String.valueOf(totalNumOfMessagesTried));
        messagesReportDoc.setCreditBefore(creditBefore);
        messagesReportDoc.setCreditAfter(creditAfter);

        messagesReportDocRepository.save(messagesReportDoc);

        return wereAllMessagesSent;
    }

    void returnCreditBalanceToCustomer(int noOfCreditsToDeduct, String userName){

        UserDoc userDoc = userDocRepository.findByUserName(userName);

        int newCreditBalance = userDoc.getCreditBalance() + noOfCreditsToDeduct;
        userDoc.setCreditBalance(newCreditBalance);

        userDocRepository.save(userDoc);
    }
}
