package com.donututils.punishhistory.roster;

import com.donututils.punishhistory.model.Note;
import com.donututils.punishhistory.model.PunishmentSnapshot;
import com.donututils.punishhistory.model.RosterEntry;
import com.donututils.punishhistory.notes.NoteStore;
import com.donututils.punishhistory.reflect.UdsBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the roster page: every player who shows up in UltimateDonutSmp's most recent
 * punishment records (up to {@code scanSize}, server-wide - not a full historical scan) plus
 * every player who has a staff note on file, newest activity first.
 */
public final class RosterBuilder {

    private RosterBuilder() {
    }

    public static List<RosterEntry> build(UdsBridge bridge, NoteStore noteStore, int scanSize) throws ReflectiveOperationException {
        Map<UUID, Integer> counts = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();
        Map<UUID, Long> lastSeen = new HashMap<>();

        List<PunishmentSnapshot> recent = bridge.getAll(null, scanSize, 0);
        for (PunishmentSnapshot snapshot : recent) {
            UUID uuid = snapshot.targetUuid();
            if (uuid == null) {
                continue;
            }
            counts.merge(uuid, 1, Integer::sum);
            names.putIfAbsent(uuid, snapshot.targetName());
            lastSeen.merge(uuid, snapshot.issuedAt(), Math::max);
        }

        for (UUID uuid : noteStore.allNotedPlayers()) {
            counts.putIfAbsent(uuid, 0);
            if (!names.containsKey(uuid)) {
                names.put(uuid, bridge.resolveTargetName(uuid));
            }
            long latestNote = 0L;
            for (Note note : noteStore.getNotes(uuid)) {
                latestNote = Math.max(latestNote, note.timestamp());
            }
            lastSeen.merge(uuid, latestNote, Math::max);
        }

        List<RosterEntry> list = new ArrayList<>();
        for (UUID uuid : names.keySet()) {
            list.add(new RosterEntry(
                    uuid,
                    names.getOrDefault(uuid, uuid.toString().substring(0, 8)),
                    counts.getOrDefault(uuid, 0),
                    noteStore.countNotes(uuid),
                    lastSeen.getOrDefault(uuid, 0L)
            ));
        }
        list.sort((a, b) -> Long.compare(b.lastActivity(), a.lastActivity()));
        return list;
    }
}
