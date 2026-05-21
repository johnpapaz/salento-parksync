package com.parksync.query;

import com.parksync.hysteresis.ParkingLotRepository;
import com.parksync.shared.ParkingStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private final ParkingStateCache stateCache;
    private final ParkingLotRepository lotRepository;

    public QueryService(ParkingStateCache stateCache, ParkingLotRepository lotRepository) {
        this.stateCache = stateCache;
        this.lotRepository = lotRepository;
    }

    public ParkingStatusResponse getStatus(String parkingLotId) {
        return new ParkingStatusResponse(
                parkingLotId,
                stateCache.getStatus(parkingLotId),
                stateCache.getLastUpdated(parkingLotId));
    }

    public List<ParkingStatusResponse> getAllStatuses() {
        return stateCache.getActiveLots().stream()
                .map(this::getStatus)
                .toList();
    }
}
