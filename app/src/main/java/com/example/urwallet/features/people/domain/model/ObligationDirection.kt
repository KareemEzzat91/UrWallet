package com.example.urwallet.features.people.domain.model

enum class ObligationDirection {
    /**
     * The other person owes money to the user (User is the creditor / مستحق لي).
     */
    OWED_TO_ME,

    /**
     * The user owes money to the other person (User is the debtor / عليّ للطرف الآخر).
     */
    I_OWE
}
