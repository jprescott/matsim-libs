/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.mobsim.deqsim;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.deqsim.util.Timer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;

import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.utils.misc.Time;

public class DEQSimulation {

	private static final Logger log = Logger.getLogger(Controler.class);
	Population population;
	NetworkLayer network;

	public DEQSimulation(final NetworkLayer network, final Population population, final Events events) {
		// constructor

		this.population = population;
		this.network = network;

		// the thread for processing the events
		SimulationParameters.setProcessEventThread(events);
		
		// initialize Simulation parameters
		SimulationParameters.setLinkCapacityPeriod(network.getCapacityPeriod());
		
		// READING SIMULATION PARAMETERS FROM CONFIG FILE
		final String DEQ_SIM = "deqSim";
		final String STUCK_TIME = "stuckTime";
		final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
		final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
		final String MINIMUM_INFLOW_CAPACITY = "minimumInFlowCapacity";
		final String CAR_SIZE = "carSize";
		final String GAP_TRAVEL_SPEED = "gapTravelSpeed";

		String stuckTime = Gbl.getConfig().findParam(DEQ_SIM, STUCK_TIME);
		String flowCapacityFactor = Gbl.getConfig().findParam(DEQ_SIM, FLOW_CAPACITY_FACTOR);
		String storageCapacityFactor = Gbl.getConfig().findParam(DEQ_SIM, STORAGE_CAPACITY_FACTOR);
		String minimumInFlowCapacity = Gbl.getConfig().findParam(DEQ_SIM, MINIMUM_INFLOW_CAPACITY);
		String carSize = Gbl.getConfig().findParam(DEQ_SIM, CAR_SIZE);
		String gapTravelSpeed = Gbl.getConfig().findParam(DEQ_SIM, GAP_TRAVEL_SPEED);

		if (stuckTime != null) {
			SimulationParameters.setStuckTime(Double.parseDouble(stuckTime));
		} else {
			log.info("parameter 'stuckTime' not defined. Using default value [s]: "
					+ SimulationParameters.getStuckTime());
		}
		
		if (flowCapacityFactor != null) {
			SimulationParameters.setFlowCapacityFactor((Double.parseDouble(flowCapacityFactor)));
		} else {
			log.info("parameter 'flowCapacityFactor' not defined. Using default value: "
					+ SimulationParameters.getFlowCapacityFactor());
		}
		
		if (storageCapacityFactor != null) {
			SimulationParameters.setStorageCapacityFactor((Double.parseDouble(storageCapacityFactor)));
		} else {
			log.info("parameter 'storageCapacityFactor' not defined. Using default value: "
					+ SimulationParameters.getStorageCapacityFactor());
		}
		
		if (minimumInFlowCapacity != null) {
			SimulationParameters.setMinimumInFlowCapacity((Double.parseDouble(minimumInFlowCapacity)));
		} else {
			log.info("parameter 'minimumInFlowCapacity' not defined. Using default value [vehicles per hour]: "
					+ SimulationParameters.getMinimumInFlowCapacity());
		}
		
		if (carSize != null) {
			SimulationParameters.setCarSize((Double.parseDouble(carSize)));
		} else {
			log.info("parameter 'carSize' not defined. Using default value [m]: "
					+ SimulationParameters.getCarSize());
		}
		
		if (gapTravelSpeed != null) {
			SimulationParameters.setGapTravelSpeed(Double.parseDouble(gapTravelSpeed));
		} else {
			log.info("parameter 'gapTravelSpeed' not defined. Using default value [m/s]: "
					+ SimulationParameters.getGapTravelSpeed());
		}
		
		// get start time
		// the semantics of this parameter needs to be defined in a consistent way
		// for this reason it is turned off at the moment
		/*
		double startTime = Gbl.getConfig().simulation().getStartTime();
		if (startTime == Time.UNDEFINED_TIME) startTime = 0.0;
		SimulationParameters.setStartTime(startTime);
		*/
		
		// read end time
		Double stopTime=Gbl.getConfig().simulation().getEndTime();
		if ((stopTime == Time.UNDEFINED_TIME) || (stopTime == 0)) stopTime = Double.MAX_VALUE;
		SimulationParameters.setMaxSimulationLength(stopTime);
		
		

		// enable testing to hook in here as a handler
		if (SimulationParameters.getTestEventHandler() != null) {
			SimulationParameters.getProcessEventThread().addHandler(
					SimulationParameters.getTestEventHandler());
		}

		if (SimulationParameters.getTestPlanPath() != null) {
			// read population
			Population pop = new Population(Population.NO_STREAMING);
			PopulationReader plansReader = new MatsimPopulationReader(pop);
			plansReader.readFile(SimulationParameters.getTestPlanPath());

			this.population = pop;

		}

		if (SimulationParameters.getTestPopulationModifier() != null) {
			this.population = SimulationParameters.getTestPopulationModifier().modifyPopulation(
					this.population);
		}

	}

	public void run() {
		Timer t = new Timer();
		t.startTimer();

		Scheduler scheduler = new Scheduler();
		SimulationParameters.setAllRoads(new HashMap<String, Road>());

		// initialize network
		Road road = null;
		for (Link link : network.getLinks().values()) {
			road = new Road(scheduler, link);
			SimulationParameters.getAllRoads().put(link.getId().toString(), road);
		}

		// initialize vehicles
		Vehicle vehicle = null;
		// the vehicle has registered itself to the scheduler
		for (Person person : this.population.getPersons().values()) {
			vehicle = new Vehicle(scheduler, person);
		}

		// just inserted to remove message in bug analysis, that vehicle
		// variable is never read
		vehicle.toString();

		scheduler.startSimulation();

		t.endTimer();
		t.printMeasuredTime("Time needed for one iteration (only DES part): ");

	}
}
