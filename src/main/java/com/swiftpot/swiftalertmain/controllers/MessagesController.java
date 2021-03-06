package com.swiftpot.swiftalertmain.controllers;

import com.swiftpot.swiftalertmain.businesslogic.MessagesLogic;
import com.swiftpot.swiftalertmain.models.BulkMessagesRequest;
import com.swiftpot.swiftalertmain.models.OutgoingPayload;
import com.swiftpot.swiftalertmain.models.SingleMessagesRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ace Programmer Rbk
 *         <Rodney Kwabena Boachie at [rodney@swiftpot.com,rbk.unlimited@gmail.com]> on
 *         02-Oct-16 @ 2:47 PM
 */
@RestController
@RequestMapping(path = "/api/v2")
public class MessagesController{

    @Autowired
    MessagesLogic messagesLogic;

    @RequestMapping(path = "/messages",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    public OutgoingPayload sendSingleMessage(@RequestBody SingleMessagesRequest singleMessagesRequest) {
        return messagesLogic.sendSingleMessage(singleMessagesRequest);
    }

    @RequestMapping(path = "/messages/bulk",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    public OutgoingPayload sendMessagesInBulk(@RequestBody BulkMessagesRequest bulkMessagesRequest) {
        return messagesLogic.sendMessagesInBulk(bulkMessagesRequest);
    }



}
