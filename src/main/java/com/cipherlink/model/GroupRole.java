package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

public enum GroupRole {
    MEMBER, ADMIN, OWNER
}
