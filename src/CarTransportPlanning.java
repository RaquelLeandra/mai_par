import java.util.ArrayList;


public class CarTransportPlanning {

	public static void main(String[] args) {
		//2 arguments are required: inputFile and outputFile
		if (args.length != 2) {
			System.out.println("ERROR: Incorrect number of arguments.\nUsage: java CarTransportPlanning inputFilename outputFilename");
			return;
		}
		//Get initial and goal states from input file
		State state = PlannerIOHelper.getInitialState(args[0]);
		GoalStack goalStack = PlannerIOHelper.getInitialGoalStack(args[0]);
		ArrayList<StackElement> plan = new ArrayList<StackElement>();
		while(!goalStack.empty()) {
			//Prevent infinite recursion
			if (goalStack.size() > 10000) {
				System.out.println("Possible goal stack overflow (more than 1000 elements). Execution stopped.");
				break;
			}
			StackElement e = goalStack.pop();
			if (e.isOperator()) {
				state.applyOperator(e);
				System.out.println("[STACK] APPLY OPERATOR TO STATE: Operator " + e.toString());
				plan.add(e);
			}
			else if (e.isCondition()) {
				if (e.isInstantiated()) {
					//If state satisfies the condition, we don't do anything
					//If it doesn't, we look for an operator that has the condition in the AddList
					System.out.println("[STACK] CHECK CONDITION: " + e.toString());
					if (!state.satisfies(e)) {
						System.out.println("Condition NOT satisfied.");
						StackElement operator = getOperatorWithConditionInAddList(state,e);
						System.out.println("[STACK] PUSH OPERATOR AND PRECONDITIONS: " + operator.toString());
						goalStack.push(operator);
						ArrayList<StackElement> preconditions = operator.getPreconditions();
						//preconditions should be already correctly ordered (heuristic)
						for (int i = 0; i < preconditions.size(); i++) {
							goalStack.push(preconditions.get(i));
						}
					}
					else {
						System.out.println("Condition satisfied.");
					}
				}
				else {
					System.out.println("[STACK] INSTANTIATE AND PROPAGATE: " + e.toString());
					boolean success = goalStack.instantiateAndPropagate(state);
					if (!success) {
						System.out.println("Instantiation was not possible");
						//push it again to diagnose the failure
						goalStack.push(e);
						break;
					}
				}
			}
			else {
				System.out.println(e.getType());
				throw new RuntimeException("Stack element type not recognized.");
			}
		}
		if (!goalStack.empty()) {
			System.out.println("Algorithm was NOT successful.");
			PlannerIOHelper.outputUnsuccessfulPlan(plan,goalStack,state,args[1]);
		}
		else {
			System.out.println("Algorithm has finished! Found plan with " + plan.size() + " operators.");
			String s = "";
			for (int i = 0; i < plan.size(); i++) {
				StackElement op = plan.get(i);
				if (i != 0) s += ",";
				s += op.getName() + "(" + String.join(",", op.getArgs()) + ")";
				for (int j = 0; j < op.getArgs().size(); ++j) {
					op.getArgs().get(j);
				}
			}
			System.out.println("Plan: " + s);
			PlannerIOHelper.outputSuccessfulPlan(plan,args[1]);
		}
		
	}

	
	
	private static StackElement getOperatorWithConditionInAddList(State state,StackElement condition) {
		StackElement operator;
		String name = condition.getName();
		//There is always one argument at least and two arguments at most
		String x = condition.getArgs().get(0);
		String y = null;
		if (condition.getArgs().size() > 1) {
			y = condition.getArgs().get(1);
		}
		//case FirstFerry
		if (name.equals("FirstFerry")) {
			if (state.satisfies(Conditions.LastDock(x))) {
				operator = Operators.BoardFirst1(x);
			}
			else {				
				//optimization: Operator can be instantiated now.
				y = state.getDockCarAfter(x);
				operator = Operators.BoardFirst2(x,y);
			}
		}
		//case NextToFerry
		else if (name.equals("NextToFerry")) {
			if (state.satisfies(Conditions.LastDock(x))) {
				operator = Operators.BoardNextTo1(x,y);
			}
			else {
				//optimization: Operator can be instantiated now.
				String z = state.getDockCarAfter(x);
				operator = Operators.BoardNextTo2(x,z,y);
			}
		}
		//case FirstDock
		else if (name.equals("FirstDock")) {
			//position in line of car x, starting at 1.
			int positionInLine = state.getPositionInLine(x);
			if (state.getNumEmptyLines() >= positionInLine - 1) {
				//optimization: Operator can be instantiated now.
				y = state.getDockCarBefore(x);
				operator = Operators.ChangeToEmptyLine(y, x);
			}
			else {
				//optimization: Operator can be partially instantiated now.
				y = state.getDockCarBefore(x);
				operator = Operators.ChangeLine(y, x, null);
			}
		}
		else {
			operator = null;
		}
		return operator;
	}

	
}
