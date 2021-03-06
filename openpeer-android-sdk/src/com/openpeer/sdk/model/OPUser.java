/*******************************************************************************
 *
 *  Copyright (c) 2014 , Hookflash Inc.
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  The views and conclusions contained in the software and documentation are those
 *  of the authors and should not be interpreted as representing official policies,
 *  either expressed or implied, of the FreeBSD Project.
 *******************************************************************************/
package com.openpeer.sdk.model;

import java.util.ArrayList;
import java.util.List;

import com.openpeer.javaapi.OPAccount;
import com.openpeer.javaapi.OPContact;
import com.openpeer.javaapi.OPIdentityContact;
import com.openpeer.sdk.app.OPDataManager;

public class OPUser {
    long mUserId;// locally maintained user id

    List<OPIdentityContact> mIdentityContacts;
    OPContact mOPContact;

    /**
     * If the user is a contact, or a stranger
     * 
     * This is used primarily in group chat to determine if a participant is a known contact.
     * 
     * @return
     */
    public boolean isContact() {
        if (mIdentityContacts != null) {
            for (OPIdentityContact contact : mIdentityContacts) {
                if (contact.getAssociatedIdentityId() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Lazy insta
     * 
     * @return
     */
    public OPContact getOPContact() {
        // Lazy creation of opcontact to avoid problem before core stack is ready.
        if (mOPContact == null) {
            OPIdentityContact contact = getPreferredContact();

            mOPContact = OPContact.createFromPeerFilePublic(OPDataManager
                    .getInstance().getSharedAccount(), contact
                    .getPeerFilePublic()
                    .getPeerFileString());
        }
        return mOPContact;
    }

    public void setOPContact(OPContact contact) {
        this.mOPContact = contact;
    }

    public List<OPIdentityContact> getIdentityContacts() {
        return mIdentityContacts;
    }

    public void setIdentityContact(List<OPIdentityContact> mIdentityContact) {
        this.mIdentityContacts = mIdentityContact;
    }

    /**
     * Used to construct a new user from incoming thread contact
     * 
     * @param contact
     * @param iContacts
     */
    public OPUser(OPContact contact, List<OPIdentityContact> iContacts) {
        this.mOPContact = contact;
        this.mIdentityContacts = iContacts;
    }

    public OPUser() {
    }

    public static OPUser fromIdentityContact(OPAccount account,
            OPIdentityContact iContact) {
        List<OPIdentityContact> identities = new ArrayList<OPIdentityContact>();
        OPContact contact = OPContact.createFromPeerFilePublic(account,
                iContact.getPeerFilePublic().getPeerFileString());

        return new OPUser(contact, identities);
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long mUserId) {
        this.mUserId = mUserId;
    }

    public String getPeerUri() {
        return getOPContact().getPeerURI();

    }

    public String getName() {
        return getPreferredContact().getName();
    }

    public String getAvatarUri() {
        return getPreferredContact().getDefaultAvatarUrl();
    }

    public OPIdentityContact getPreferredContact() {
        return mIdentityContacts.get(0);
    }

    public boolean isSame(OPContact contact) {
        // TODO Auto-generated method stub
        return contact.getPeerURI().equals(getOPContact().getPeerURI());
    }

    @Override
    public boolean equals(Object o) {
        // TODO Auto-generated method stub
        return o instanceof OPUser && ((OPUser) o).getUserId() == this.mUserId;
    }

}
