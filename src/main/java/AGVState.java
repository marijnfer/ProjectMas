/**
 * IDLE:                   doing nothing
 * TOINBOUND               driving from a deliveringPoint to an InboundPoint
 * DELEVERING:             driving from the last assembly that needed to be visited towards an DeliveryPoint
 * DRIVINGTOASSEMBLYcross: driving towards a crossroad in the assembly
 * DRIVINGTOASSEMBLY:      driving towards an assemblyPoint (short connections)
 * DRIVINGAWAYASSEMBLY:    driving away from an assemblyPoint(short connections)
 * INBOUNDTOASSEMBLY:      driving from the inbound to the assembly
 * WAIT:                   waiting
 */


public enum AGVState {
    IDLE, TOINBOUND, DELEVERING, DRIVINGTOASSEMBLYcross,DRIVINGTOASSEMBLY,DRIVINGAWAYASSEMBLY, INBOUNDTOASSEMBLY,WAIT};

