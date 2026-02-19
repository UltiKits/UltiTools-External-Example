package com.example.ultitoolsext.data;

import com.ultikits.ultitools.abstracts.data.BaseDataEntity;
import com.ultikits.ultitools.annotations.Column;
import com.ultikits.ultitools.annotations.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("visitor_records")
public class VisitorRecord extends BaseDataEntity<String> {

    @Column("player_name")
    private String playerName;

    @Column("visit_count")
    private int visitCount;

    @Column("last_visit")
    private long lastVisit;

    public VisitorRecord(String playerName) {
        this.playerName = playerName;
        this.visitCount = 1;
        this.lastVisit = System.currentTimeMillis();
    }

    public void incrementVisit() {
        this.visitCount++;
        this.lastVisit = System.currentTimeMillis();
    }
}
