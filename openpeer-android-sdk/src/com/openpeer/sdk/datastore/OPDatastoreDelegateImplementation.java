package com.openpeer.sdk.datastore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.openpeer.javaapi.OPAccount;
import com.openpeer.javaapi.OPContact;
import com.openpeer.javaapi.OPIdentity;
import com.openpeer.javaapi.OPIdentityContact;
import com.openpeer.javaapi.OPMessage;
import com.openpeer.javaapi.OPRolodexContact;
import com.openpeer.javaapi.OPRolodexContact.OPAvatar;
import com.openpeer.sdk.app.OPDataManager;
import com.openpeer.sdk.app.OPSession;
import com.openpeer.sdk.datastore.DatabaseContracts.AvatarEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.ContactEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.ContactsViewEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.ConversationWindowEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.IdentityContactEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.IdentityEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.MessageEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.UserEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.WindowParticipantEntry;
import com.openpeer.sdk.datastore.DatabaseContracts.WindowViewEntry;
import com.openpeer.sdk.model.OPHomeUser;
import com.openpeer.sdk.model.OPUser;

import static com.openpeer.sdk.datastore.DatabaseContracts.*;

/**
 * The data being stored in preference: -- Relogin information for account -- stableId
 * 
 * The data being stored in database Contacts Conversation history Call history
 * 
 */
public class OPDatastoreDelegateImplementation implements OPDatastoreDelegate {
	private static final String PREF_DATASTORE = "model_data";
	private static final String PREF_KEY_RELOGIN_INFO = "relogin_info";
	private static final String PREF_KEY_HOMEUSER_STABLEID = "homeuser_stable_id";
	private static final String TAG = OPDatastoreDelegateImplementation.class.getSimpleName();

	private static OPDatastoreDelegateImplementation instance;

	private SharedPreferences mPreferenceStore;
	Context mContext;

	private OPDatastoreDelegateImplementation() {

	}

	private OPDatastoreDelegateImplementation(Context context) {
		mContext = context;

		mPreferenceStore = context.getSharedPreferences(PREF_DATASTORE,
				Context.MODE_PRIVATE);
	}

	public static OPDatastoreDelegateImplementation getInstance() {
		if (instance == null) {
			instance = new OPDatastoreDelegateImplementation();
		}
		return instance;
	}

	public OPDatastoreDelegateImplementation init(Context context) {
		mContext = context;

		mPreferenceStore = context.getSharedPreferences(PREF_DATASTORE,
				Context.MODE_PRIVATE);
		return this;// fluent API

	}

	@Override
	public String getReloginInfo() {
		return mPreferenceStore.getString(PREF_KEY_RELOGIN_INFO, null);
	}

	@Override
	public boolean saveOrUpdateAccount(OPAccount account) {
		Log.d("test",
				"DatastoreDelegate saving account id " + account.getStableID()
						+ " relogin " + account.getReloginInformation());
		SharedPreferences.Editor editor = mPreferenceStore.edit();
		editor.putString(PREF_KEY_RELOGIN_INFO, account.getReloginInformation());
		editor.putLong(PREF_KEY_HOMEUSER_STABLEID, account.getStableID());
		editor.apply();
		return true;
	}

	@Override
	public boolean saveOrUpdateIdentities(List<OPIdentity> identities,
			long accountId) {
		for (OPIdentity identity : identities) {
			saveOrUpdateIdentity(identity, accountId);
		}

		return true;
	}

	@Override
	public boolean saveOrUpdateContacts(
			List<? extends OPRolodexContact> contacts, long identityId) {
		for (OPRolodexContact contact : contacts) {
			Log.d("OPDatastoreDelegateImplementation", "saveOrUpdateContacts "
					+ contact + " identityId " + identityId);
			saveOrUpdateContact(contact, identityId);
		}
		mContext.getContentResolver().notifyChange(OPContentProvider.getContentUri(ContactsViewEntry.URI_PATH_INFO), null);
		return true;
	}

	@Override
	public boolean saveOrUpdateIdentity(OPIdentity identity, long accountId) {
		OPIdentityContact selfContact = identity.getSelfIdentityContact();
		selfContact.setUserId(0);
		ContentValues values = new ContentValues();
		values.put(IdentityEntry.COLUMN_NAME_IDENTITY_ID,
				identity.getStableID());
		values.put(IdentityEntry.COLUMN_NAME_IDENTITY_PROVIDER,
				identity.getIdentityProviderDomain());
		values.put(DatabaseContracts.COLUMN_NAME_IDENTITY_URI,
				identity.getIdentityURI());
		values.put(IdentityEntry.COLUMN_NAME_IDENTITY_CONTACT_ID,
				selfContact.getStableID());
		String whereClause = IdentityEntry.COLUMN_NAME_IDENTITY_ID + "=?";
		String args[] = new String[] { identity.getIdentityURI() };
		if (upsert(IdentityEntry.TABLE_NAME, values, whereClause, args)) {
			return saveOrUpdateContact(identity.getSelfIdentityContact(),
					identity.getStableID());
		} else {
			return false;
		}
	}

	@Override
	public boolean saveOrUpdateContact(OPRolodexContact contact, long identityId) {
		ContentValues values = new ContentValues();
		if (contact instanceof OPIdentityContact) {
			values.put(ContactsViewEntry.COLUMN_NAME_STABLE_ID, ((OPIdentityContact) contact).getStableID());
		}
		values.put(ContactEntry.COLUMN_NAME_CONTACT_NAME, contact.getName());
		values.put(ContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID, identityId);
		values.put(DatabaseContracts.COLUMN_NAME_IDENTITY_URI, contact.getIdentityURI());
		values.put(ContactEntry.COLUMN_NAME_IDENTITY_PROVIDER, contact.getIdentityProvider());
		values.put(ContactEntry.COLUMN_NAME_URL, contact.getProfileURL());
		values.put(ContactEntry.COLUMN_NAME_VPROFILE_URL, contact.getVProfileURL());

		String args[] = new String[] { contact.getIdentityURI() };
		boolean result = upsert(
				DatabaseContracts.ContactEntry.TABLE_NAME,
				values,
				ContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID + "="
						+ identityId + " and "
						+ DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", args);

		if (result && contact.getAvatars() != null) {
			// insert or update avatar
			for (OPAvatar avatar : contact.getAvatars()) {
				delete(AvatarEntry.TABLE_NAME, DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", args);
				ContentValues avatarValues = new ContentValues();
				avatarValues.put(DatabaseContracts.COLUMN_NAME_IDENTITY_URI, contact.getIdentityURI());
				avatarValues.put(AvatarEntry.COLUMN_NAME_AVATAR_NAME, avatar.getName());
				avatarValues.put(DatabaseContracts.COLUMN_NAME_AVATAR_URI, avatar.getURL());
				avatarValues.put(AvatarEntry.COLUMN_NAME_WIDTH, avatar.getWidth());
				avatarValues.put(AvatarEntry.COLUMN_NAME_HEIGHT, avatar.getHeight());

				insert(AvatarEntry.TABLE_NAME, avatarValues);
			}
		}
		if (contact instanceof OPIdentityContact) {
			OPIdentityContact ic = (OPIdentityContact) contact;
			ContentValues icValues = new ContentValues();

			icValues.put(IdentityContactEntry.COLUMN_NAME_USER_ID, ic.getUserId());
			icValues.put(DatabaseContracts.COLUMN_NAME_IDENTITY_URI, ic.getIdentityURI());

			icValues.put(IdentityContactEntry.COLUMN_NAME_STABLE_ID, ic.getStableID());
			icValues.put(IdentityContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID, identityId);
			icValues.put(IdentityContactEntry.COLUMN_NAME_PEERFILE_PUBLIC, ic.getPeerFilePublic().getPeerFileString());

			icValues.put(IdentityContactEntry.COLUMN_NAME_IDENTITY_PROOF_BUNDLE, ic.getIdentityProofBundle());
			icValues.put(IdentityContactEntry.COLUMN_NAME_PRORITY, ic.getPriority());
			icValues.put(IdentityContactEntry.COLUMN_NAME_LAST_UPDATE_TIME, ic.getLastUpdated().toMillis(false));
			icValues.put(IdentityContactEntry.COLUMN_NAME_EXPIRE, ic.getExpires().toMillis(false));
			args = new String[] { ic.getIdentityURI() };
			return upsert(
					IdentityContactEntry.TABLE_NAME,
					icValues,
					IdentityContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID
							+ "=" + identityId + " and "
							+ DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?",
					args);

		} else {
			return true;
		}

	}

	@Override
	public boolean flushContactsForIdentity(long id) {
		String selection = IdentityContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID
				+ "=" + id;
		String cSelection = ContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID
				+ "=" + id;
		// mOpenHelper.getWritableDatabase().
		delete(
				IdentityContactEntry.TABLE_NAME, selection, null);
		delete(ContactEntry.TABLE_NAME,
				cSelection, null);
		return true;
	}

	@Override
	public boolean deleteContact(String identityUri) {
		String whereArgs[] = new String[] { identityUri };
		delete(UserEntry.TABLE_NAME, DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", whereArgs);
		delete(ContactEntry.TABLE_NAME,
				DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", whereArgs);
		delete(IdentityContactEntry.TABLE_NAME, DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", whereArgs);
		delete(AvatarEntry.TABLE_NAME, DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", whereArgs);
		return true;
	}

	@Override
	public OPHomeUser getHomeUser() {
		String reloginInfo = this.mPreferenceStore.getString(
				PREF_KEY_RELOGIN_INFO, null);
		if (reloginInfo != null) {
			return new OPHomeUser(reloginInfo, mPreferenceStore.getLong(
					PREF_KEY_HOMEUSER_STABLEID, 0L));
		}
		return null;
	}

	@Override
	public boolean saveHomeUser(OPHomeUser user) {
		SharedPreferences.Editor editor = mPreferenceStore.edit();
		editor.putString(PREF_KEY_RELOGIN_INFO, user.getReloginInfo());
		editor.putLong(PREF_KEY_HOMEUSER_STABLEID, user.getStableId());
		return true;
	}

	public String getDownloadedContactsVersion(long identityId) {
		String version = null;
		Cursor cursor = query(IdentityEntry.TABLE_NAME,
				new String[] { IdentityEntry.COLUMN_NAME_IDENTITY_CONTACTS_VERSION },
				IdentityEntry.COLUMN_NAME_IDENTITY_ID + "="
						+ identityId, null);
		if (cursor != null) {
			cursor.moveToFirst();
			version = cursor
					.getString(cursor
							.getColumnIndex(IdentityEntry.COLUMN_NAME_IDENTITY_CONTACTS_VERSION));
		}
		Log.d("test", "datastore contacts version " + version);
		return version;
	}

	@Override
	public List<OPRolodexContact> getOPContacts(String identityId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDownloadedContactsVersion(long identityId, String version) {
		ContentValues values = new ContentValues();
		values.put(IdentityEntry.COLUMN_NAME_IDENTITY_CONTACTS_VERSION, version);
		String whereClause = IdentityEntry.COLUMN_NAME_IDENTITY_ID + "="
				+ identityId;
		long rowId = update(
				IdentityEntry.TABLE_NAME, values, whereClause, null);
		Log.d("test", "setDownloadedContactsVersion " + rowId + " version "
				+ version + " id " + identityId);
	}

	@Override
	public OPIdentityContact getIdentityContact(String identityContactId) {
		String iSelection = IdentityContactEntry.COLUMN_NAME_STABLE_ID + "=?";
		String cSelection = ContactsViewEntry.COLUMN_NAME_STABLE_ID + "=?";
		String[] selectionParams = new String[] { identityContactId };

		Cursor iCursor = query(IdentityContactEntry.TABLE_NAME, null, iSelection, selectionParams);
		Cursor cCursor = query(ContactEntry.TABLE_NAME, null, cSelection, selectionParams);
		if (cCursor != null && iCursor != null) {
			cCursor.moveToFirst();
			iCursor.moveToFirst();
			return identityContactFromCursor(contactFromCursor(cCursor),
					iCursor);
		}
		return null;

	}

	private OPRolodexContact contactFromCursor(Cursor cursor) {
		int identityUrlIndex = cursor
				.getColumnIndex(COLUMN_NAME_IDENTITY_URI);
		int identityProviderIndex = cursor
				.getColumnIndex(ContactEntry.COLUMN_NAME_IDENTITY_PROVIDER);
		int nameIndex = cursor
				.getColumnIndex(ContactEntry.COLUMN_NAME_CONTACT_NAME);
		int profileURLIndex = cursor
				.getColumnIndex(ContactEntry.COLUMN_NAME_URL);
		int vprofileURLIndex = cursor
				.getColumnIndex(ContactEntry.COLUMN_NAME_VPROFILE_URL);
		int assoiatedIdentityIdIndex = cursor
				.getColumnIndex(ContactEntry.COLUMN_NAME_ASSOCIATED_IDENTITY_ID);

		OPRolodexContact contact = new OPRolodexContact(
				cursor.getString(identityUrlIndex),
				cursor.getString(identityProviderIndex),
				cursor.getString(nameIndex), cursor.getString(profileURLIndex),
				cursor.getString(vprofileURLIndex), null,
				cursor.getLong(assoiatedIdentityIdIndex));

		contact.setAvatars(this.getAvatars(contact.getIdentityURI()));
		String stableIdentityId = cursor.getString(cursor
				.getColumnIndex(ContactsViewEntry.COLUMN_NAME_STABLE_ID));
		if (stableIdentityId != null) {
			Log.d("test", "retrieving identity contact of id "
					+ stableIdentityId);
			Cursor iCursor = query(
					IdentityContactEntry.TABLE_NAME, null,
					IdentityContactEntry.COLUMN_NAME_STABLE_ID + "=?",
					new String[] { stableIdentityId });
			if (iCursor != null) {
				iCursor.moveToFirst();
				contact = identityContactFromCursor(contact, iCursor);
				iCursor.close();
			}
		}
		return contact;
	}

	private OPIdentityContact identityContactFromCursor(
			OPRolodexContact contact, Cursor iCursor) {
		OPIdentityContact iContact = new OPIdentityContact(contact);
		return iContact
				.setIdentityParams(
						iCursor.getString(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_STABLE_ID)),
						iCursor.getString(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_PEERFILE_PUBLIC)),
						iCursor.getString(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_IDENTITY_PROOF_BUNDLE)),
						iCursor.getInt(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_PRORITY)),
						iCursor.getInt(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_WEIGHT)),
						iCursor.getLong(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_LAST_UPDATE_TIME)),
						iCursor.getLong(iCursor
								.getColumnIndex(IdentityContactEntry.COLUMN_NAME_EXPIRE)),
						iCursor.getLong(0));
	}

	@Override
	public List<OPAvatar> getAvatars(String contactId) {
		String args[] = new String[] { contactId };
		Cursor cursor = query(AvatarEntry.TABLE_NAME, null, DatabaseContracts.COLUMN_NAME_IDENTITY_URI + "=?", args);
		if (cursor != null) {
			List<OPAvatar> avatars = new ArrayList<OPAvatar>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				OPAvatar avatar = new OPAvatar(
						cursor.getString(cursor
								.getColumnIndex(AvatarEntry.COLUMN_NAME_AVATAR_NAME)),
						cursor.getString(cursor
								.getColumnIndex(DatabaseContracts.COLUMN_NAME_AVATAR_URI)),
						0, 0);
				avatars.add(avatar);
				cursor.moveToNext();
			}
			cursor.close();
			return avatars;
		}
		return null;
	}

	@Override
	public boolean saveMessage(OPMessage message, long windowId, String threadId) {
		Log.d("TODO", "OPDatastoreDelegate saveMessage " + message.getMessage()
				+ " sessionId " + windowId);
		ContentValues values = new ContentValues();
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_ID, message.getMessageId());
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_TEXT, message.getMessage());
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_TIME, message.getTime().toMillis(true));

		values.put(MessageEntry.COLUMN_NAME_MESSAGE_TYPE,
				message.getMessageType());
		values.put(MessageEntry.COLUMN_NAME_SENDER_ID, message.getSenderId());
		values.put(MessageEntry.COLUMN_NAME_WINDOW_ID, windowId);
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_READ, message.isRead() ? 1 : 0);

		String url = DatabaseContracts.MessageEntry.CONTENT_ID_URI_BASE + "window/" + windowId;
		// mContext.getContentResolver().notifyChange(Uri.parse(url), null);
		Uri uri = mContext.getContentResolver().insert(Uri.parse(url), values);
		if (uri != null) {
			Log.d("test", "now notify change for " + url);
			mContext.getContentResolver().notifyChange(OPContentProvider.getContentUri(WindowViewEntry.URI_PATH_INFO), null);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateMessageDeliveryStatus(long windowId, String messageId, int deliveryStatus, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_ID, messageId);
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_TIME, updateTime);
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_DELIVERY_STATUS, deliveryStatus);
		String url = DatabaseContracts.MessageEntry.CONTENT_ID_URI_BASE + "window/" + windowId;
		// mContext.getContentResolver().notifyChange(Uri.parse(url), null);
		Uri uri = mContext.getContentResolver().insert(Uri.parse(url), values);
		if (uri != null) {
			Log.d("test", "now notify change for " + url);
			mContext.getContentResolver().notifyChange(OPContentProvider.getContentUri(WindowViewEntry.URI_PATH_INFO), null);
			return true;
		}
		return false;
	}

	@Override
	public void saveWindow(long windowId, List<OPUser> userList) {
		ContentValues values = new ContentValues();
		values.put(ConversationWindowEntry.COLUMN_NAME_WINDOW_ID, windowId);
		Cursor cursor = mContext.getContentResolver().query(OPContentProvider.getContentUri(WindowViewEntry.URI_PATH_INFO), null,
				WindowViewEntry.COLUMN_NAME_WINDOW_ID + "=" + windowId, null, null);
		if (cursor != null && cursor.getCount() > 0) {
			Log.d("test", "saveWindow window exists " + windowId);
			return;
		}
		Uri uri = mContext.getContentResolver().insert(OPContentProvider.getContentUri(ConversationWindowEntry.URI_PATH_INFO), values);
		if (uri != null) {
			Log.d("test", "Inserted window " + Arrays.deepToString(values.valueSet().toArray()));

			// now insert the participants
			ContentValues contentValues[] = new ContentValues[userList.size()];
			for (int i = 0; i < userList.size(); i++) {
				OPUser user = userList.get(i);
				contentValues[i] = new ContentValues();
				contentValues[i].put(WindowParticipantEntry.COLUMN_NAME_WINDOW_ID, windowId);

				contentValues[i].put(WindowParticipantEntry.COLUMN_NAME_USER_ID, user.getUserId());
				contentValues[i].put(WindowParticipantEntry.COLUMN_NAME_USER_NAME, user.getName());
				contentValues[i].put(DatabaseContracts.COLUMN_NAME_AVATAR_URI, user.getAvatarUri());
				// contentValues[i].put(ConversationWindowEntry., windowId);

			}
			int count = mContext.getContentResolver().bulkInsert(OPContentProvider.getContentUri(WindowParticipantEntry.URI_PATH_INFO),
					contentValues);
			Log.d("test", "Inserted window participants " + count + " values " + Arrays.deepToString(contentValues));

			mContext.getContentResolver().notifyChange(OPContentProvider.getContentUri(WindowViewEntry.URI_PATH_INFO), null);
		}
	}

	@Override
	public void saveOrUpdateUsers(List<OPIdentityContact> iContacts, long associatedIdentityId) {
		for (OPIdentityContact contact : iContacts) {
			ContentValues values = new ContentValues();
			values.put(DatabaseContracts.COLUMN_NAME_AVATAR_URI, contact.getDefaultAvatarUrl());
			values.put(COLUMN_NAME_IDENTITY_URI, contact.getIdentityURI());
			values.put(UserEntry.COLUMN_NAME_STABLE_ID, contact.getStableID());
			values.put(UserEntry.COLUMN_NAME_USER_NAME, contact.getName());
			OPContact oContact = OPContact.createFromPeerFilePublic(OPDataManager.getInstance().getSharedAccount(), contact
					.getPeerFilePublic().getPeerFileString());
			values.put(UserEntry.COLUMN_NAME_PEER_URI, oContact.getPeerURI());

			Uri uri = mContext.getContentResolver().insert(OPContentProvider.getContentUri(UserEntry.URI_PATH_INFO), values);
			if (uri != null) {
				// this is a new user
				long userId = Long.parseLong(uri.getLastPathSegment());
				contact.setUserId(userId);
				saveOrUpdateContact(contact, associatedIdentityId);

			} else {
				String selection = UserEntry.COLUMN_NAME_STABLE_ID + "=?" + " or " +
						UserEntry.COLUMN_NAME_PEER_URI + "=?" + " or " +
						COLUMN_NAME_IDENTITY_URI + "=?";
				String args[] = { contact.getStableID(), oContact.getPeerURI(), contact.getIdentityURI() };
				mContext.getContentResolver().update(OPContentProvider.getContentUri(UserEntry.URI_PATH_INFO), values, selection, args);
				// TODO:update contacts with userId
			}
			mContext.getContentResolver().notifyChange(OPContentProvider.getContentUri(ContactsViewEntry.URI_PATH_INFO), null);

		}
	}

	/**
	 * Save a user from incoming thread. If the use is already in database, set the userId and update user info
	 * 
	 * @param user
	 */
	@Override
	public OPUser saveUser(OPUser user) {
		long userID = -1;
		ContentValues values = new ContentValues();
		if (user.getLockboxStableId() == null) {
			Log.d("test", "user doesn't have a stableId");
			return getUserByPeerUri(user.getPeerUri());
		}
		if (user.getAvatarUri() != null) {
			values.put(DatabaseContracts.COLUMN_NAME_AVATAR_URI, user.getAvatarUri());
		}

		values.put(COLUMN_NAME_IDENTITY_URI, user.getIdentityUri());
		values.put(UserEntry.COLUMN_NAME_STABLE_ID, user.getLockboxStableId());
		if (user.getName() != null) {
			values.put(UserEntry.COLUMN_NAME_USER_NAME, user.getName());
		}
		values.put(UserEntry.COLUMN_NAME_PEER_URI, user.getPeerUri());
		Log.d("test", "saveUser values " + Arrays.deepToString(values.valueSet().toArray()));

		String selection = UserEntry.COLUMN_NAME_STABLE_ID + "=?" + " or " +
				UserEntry.COLUMN_NAME_PEER_URI + "=?" + " or " +
				COLUMN_NAME_IDENTITY_URI + "=?";
		String aueryArgs[] = { user.getLockboxStableId(), user.getPeerUri(), user.getIdentityUri() };
		Cursor cursor = mContext.getContentResolver().query(OPContentProvider.getContentUri(UserEntry.URI_PATH_INFO), null, selection,
				aueryArgs, null);
		if (cursor != null && cursor.getCount() > 0) {
			// TODO: Compare and Update
			cursor.moveToFirst();
			userID = cursor.getLong(0);
			selection = UserEntry.COLUMN_NAME_STABLE_ID + "=" + userID;
			cursor.close();
			int count = mContext.getContentResolver().update(OPContentProvider.getContentUri(UserEntry.URI_PATH_INFO), values,
					"_id=" + userID, null);

		} else {
			// Insert
			Uri uri = mContext.getContentResolver().insert(OPContentProvider.getContentUri(UserEntry.URI_PATH_INFO), values);
			if (uri != null) {
				// this is a new user
				userID = Long.parseLong(uri.getLastPathSegment());
				// saveOrUpdateContact(contact, associatedIdentityId);
				Log.d(TAG, "saveUser inserted " + userID);

			}
		}
		user.setUserId(userID);
		return user;

	}

	OPUser getUserByPeerUri(String peerUri) {
		String selection = ContactsViewEntry.COLUMN_NAME_USER_ID + " in (select _id from " + UserEntry.TABLE_NAME + " where "
				+ UserEntry.COLUMN_NAME_PEER_URI + "=?)";
		Cursor cursor = mContext.getContentResolver().query(OPContentProvider.getContentUri(ContactsViewEntry.URI_PATH_INFO), null,
				selection, new String[] { peerUri }, null);
		Log.d("test", "getUserByPeerUri " + cursor.getCount());
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			return OPUser.fromDetailCursor(cursor);
		} else {
			return null;
		}
	}

	@Override
	public List<OPUser> getUsers(long[] userIDs) {
		List<OPUser> users = new ArrayList<OPUser>();
		for (long userId : userIDs) {
			Cursor cursor = mContext.getContentResolver().query(OPContentProvider.getContentUri(ContactsViewEntry.URI_PATH_INFO), null,
					ContactsViewEntry.COLUMN_NAME_USER_ID + "=" + userId, null, null);
			OPUser user = OPUser.fromDetailCursor(cursor);
			users.add(user);
		}
		return users;
	}

	@Override
	public void markMessagesRead(long windowId) {
		ContentValues values = new ContentValues();
		values.put(MessageEntry.COLUMN_NAME_MESSAGE_READ, 1);
		String where = MessageEntry.COLUMN_NAME_WINDOW_ID + "=" + windowId + " and " + MessageEntry.COLUMN_NAME_MESSAGE_READ + "=0";
		String url = DatabaseContracts.MessageEntry.CONTENT_ID_URI_BASE + "window/" + windowId;
		int count = mContext.getContentResolver().update(Uri.parse(url), values, where, null);
		Log.d("test", "markMessagesRead update count " + count);
	}

	int delete(String tableName, String whereClause,
			String[] whereArgs) {
		return mContext.getContentResolver().delete(OPContentProvider.getContentUri("/" + tableName), whereClause, whereArgs);

	}

	Uri insert(String tableName, ContentValues values) {
		return mContext.getContentResolver().insert(OPContentProvider.getContentUri("/" + tableName), values);

	}

	int update(String tableName, ContentValues values, String whereClause,
			String[] whereArgs) {
		Uri uri = OPContentProvider.getContentUri("/" + tableName);
		return mContext.getContentResolver().update(uri, values, whereClause, whereArgs);

	}

	Cursor query(String tableName, String columns[], String whereClause,
			String[] whereArgs) {
		Uri uri = OPContentProvider.getContentUri("/" + tableName);

		return mContext.getContentResolver().query(uri, columns, whereClause, whereArgs, null);
	}

	boolean upsert(String tableName, ContentValues values, String whereClause,
			String[] whereArgs) {
		Uri uri = OPContentProvider.getContentUri("/" + tableName);
		Cursor cursor = mContext.getContentResolver().query(uri, null, whereClause, whereArgs, null);
		if (cursor != null && cursor.getCount() > 0) {
			int updatedCount = mContext.getContentResolver().update(uri, values, whereClause, whereArgs);
			return updatedCount > 0;
		} else {
			Uri _uri = mContext.getContentResolver().insert(uri, values);
			return _uri != null;
		}

	}

	@Override
	public boolean deleteIdentity(long id) {
		// TODO Auto-generated method stub
		return false;
	}

}
