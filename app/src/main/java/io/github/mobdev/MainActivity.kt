package io.github.mobdev

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class Contact(
    val name: String?,
    val phoneNumber: String?,
    val email: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContactsScreen()
            }
        }
    }
}

@Composable
fun ContactsScreen() {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(context.hasContactsPermission())
    }

    var contacts by remember {
        mutableStateOf(emptyList<Contact>())
    }

    var selectedContact by remember {
        mutableStateOf<Contact?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        contacts = if (granted) context.fetchAllContacts() else emptyList()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && contacts.isEmpty()) {
            contacts = context.fetchAllContacts()
        }
    }

    Scaffold { paddingValues ->
        ScreenContent(
            paddingValues = paddingValues,
            hasPermission = hasPermission,
            contacts = contacts,
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onContactClick = { contact ->
                selectedContact = contact
            }
        )
    }

    selectedContact?.let { contact ->
        ContactDetailsDialog(
            contact = contact,
            onDismiss = { selectedContact = null }
        )
    }
}

@Composable
fun ScreenContent(
    paddingValues: PaddingValues,
    hasPermission: Boolean,
    contacts: List<Contact>,
    onRequestPermission: () -> Unit,
    onContactClick: (Contact) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        if (!hasPermission) {
            PermissionContent(onRequestPermission = onRequestPermission)
        } else {
            ContactsList(
                contacts = contacts,
                onContactClick = onContactClick
            )
        }
    }
}

@Composable
fun PermissionContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.permission_message),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}

@Composable
fun ContactsList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.screen_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (contacts.isEmpty()) {
            Text(text = stringResource(R.string.no_contacts))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = contact.name ?: stringResource(R.string.unknown_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ContactDetailsDialog(
    contact: Contact,
    onDismiss: () -> Unit
) {
    val phone = contact.phoneNumber ?: stringResource(R.string.no_phone)
    val email = contact.email ?: stringResource(R.string.no_email)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = contact.name ?: stringResource(R.string.unknown_name))
        },
        text = {
            Column {
                Text(text = stringResource(R.string.contact_details))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.phone_value, phone))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.email_value, email))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}

fun Context.hasContactsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    val result = mutableListOf<Contact>()

    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    ).use { cursor ->
        if (cursor == null) return emptyList()

        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            )

            val name = cursor.getStringOrNull(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            )

            val phoneNumber = cursor.getStringOrNull(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            )

            val email = fetchEmailByContactId(contactId)

            result.add(
                Contact(
                    name = name,
                    phoneNumber = phoneNumber,
                    email = email
                )
            )
        }
    }

    return result.distinctBy { Triple(it.name, it.phoneNumber, it.email) }
}

@SuppressLint("Range")
fun Context.fetchEmailByContactId(contactId: Long): String? {
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    ).use { cursor ->
        if (cursor == null || !cursor.moveToFirst()) return null

        return cursor.getStringOrNull(
            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        )
    }
}

fun Cursor.getStringOrNull(index: Int): String? {
    return if (index >= 0 && !isNull(index)) getString(index) else null
}