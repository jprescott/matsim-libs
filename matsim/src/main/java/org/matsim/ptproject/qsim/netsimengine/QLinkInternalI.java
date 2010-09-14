package org.matsim.ptproject.qsim.netsimengine;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.ptproject.qsim.interfaces.QLink;
import org.matsim.ptproject.qsim.interfaces.QVehicle;

public abstract class QLinkInternalI extends QBufferItem implements QLink {
	// yyyy this class needs to be public with some of the traffic signal code, but I do not understand why.  kai, aug'10

	// for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	abstract void setQSimEngine(QSimEngine qsimEngine);

	protected abstract boolean moveLink(double now);

	abstract boolean hasSpace();

	abstract void clearVehicles();

	abstract QVehicle removeParkedVehicle(Id vehicleId);
	// in contrast to "addParkedVehicle", this here does not need to be public since it is only used internally.  kai, aug'10

	abstract void activateLink();

	abstract void addFromIntersection(final QVehicle veh);

	abstract QSimEngine getQSimEngine() ;
	
//	@Deprecated // not needed here (I think)
//	public abstract LinkedList<QVehicle> getVehQueue() ;
	
	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}
}
