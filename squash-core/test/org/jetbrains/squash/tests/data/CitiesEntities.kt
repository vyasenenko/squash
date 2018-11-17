package org.jetbrains.squash.tests.data

import java.io.*

interface City {

    val id: Int
    val name: String
}

interface InhabitedCity : City {

    val citizens: List<Citizen>
}

interface Citizen {
    val id: String
    val name: String
    val city: InhabitedCity?
    val data: List<Data>
}

interface Data {
    val comment: String
    val value: DataKind
    val image: InputStream?
}
