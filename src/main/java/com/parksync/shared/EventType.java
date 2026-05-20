package com.parksync.shared;

public enum EventType {
    ENTRY,          // +1 ingreso
    EXIT,           // -1 salida
    FORCE_FULL,     // botón de pánico (RTF-06, RTF-17)
    RECALIBRATION   // set_absolute_value (RTF-07)
}
