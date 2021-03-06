package nlp.app.math.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.asu.nlu.common.ds.AnnotatedSentence;
import edu.asu.nlu.common.util.POSUtil;
import edu.stanford.nlp.ling.CoreLabel;
import nlp.app.math.core.ProblemRepresentation;
import nlp.app.math.core.Quantity;
import nlp.app.math.util.TypeDetecter;

/**
 * @author Arindam
 * This class is responsible for finding the unknowns 
 * in the problems.
 */
public class UnknownFinder {
	private TypeDetecter typeDetecter;
	private AssociatedWordFinder associatedWordFinder;

	public UnknownFinder(boolean debug){
		this.typeDetecter = new TypeDetecter();
		this.associatedWordFinder = new AssociatedWordFinder(debug);
	}

	/**
	 * It takes a problem containing single or multiple questions
	 * For each question it finds out 
	 * @param p the input problem
	 */
	public void findUnknowns(ProblemRepresentation p) throws RuntimeException{
		int sId=0;
		ArrayList<CoreLabel> prevType = null;
		for(AnnotatedSentence s: p.getAnnotatedSentences()){
			boolean isQuestion = this.isAQuestion(s);
			sId++;
			/**
			 * For each number in the text it creates a quantity constant
			 * add saves it attributes and it's position in the text
			 */
			for(CoreLabel token: s.getTokenSequence()){
				// true if it's a number
				if(s.getPOS(token).equalsIgnoreCase("cd")){

					/**
					 * create a constant quantity
					 * saves it value
					 * it's position
					 */
					Quantity quantity = p.addConstantQuantity(s.getWord(token),sId,token.index());

					//add type (closest noun phrase)	
					ArrayList<CoreLabel> type = this.typeDetecter.findType(token, s, prevType);


					/**
					 * add associated verb
					 * 
					 */

					ArrayList<Integer> typeIds = new ArrayList<Integer>();
					typeIds.add(token.index());
					for(CoreLabel l: type)
						typeIds.add(l.index());

					Set<Integer> verb = this.associatedWordFinder.findAssociatedWord(typeIds,s,"vb");

					Set<Integer> nsubj = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "nsubj");
					Set<Integer> iobj = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "iobj");
					Set<Integer> prep_of = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "prep_of");
					Set<Integer> prep_to = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "prep_to");
					Set<Integer> nmod = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "nmod");
					Set<Integer> dobj = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "dobj");
					Set<Integer> tmod = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "tmod");
					Set<Integer> amod = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "amod");
					Set<Integer> xcomp = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "xcomp");
					Set<Integer> dep = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "dep");

					Set<Integer> union =  new HashSet<Integer>();
					union.addAll(typeIds);
					union.addAll(prep_of);
					Set<Integer> prep_in = this.associatedWordFinder.findAssociatedWordWithRel(verb,
							new ArrayList<Integer>(union), s, "prep_in");
					Set<Integer> ccomp = this.associatedWordFinder.findAssociatedWordWithRel(verb, typeIds, s, "ccomp");
					union.clear();
					union.addAll(typeIds);
					union.addAll(ccomp);
					Set<Integer> advmod = this.associatedWordFinder.findAssociatedWordWithRel(verb,
							new ArrayList<Integer>(union), s, "advmod");
					Set<Integer> prep_in_amod = this.associatedWordFinder.findAssociatedWordWithRel(new HashSet<Integer>(),
							new ArrayList<Integer>(prep_in), s, "amod");

					quantity.setContext("verb", verb, s);
					quantity.setContext("amod", amod, s);
					quantity.setContext("nsubj", nsubj, s);
					quantity.setContext("tmod", tmod, s);
					quantity.setContext("iobj", iobj, s);
					quantity.setContext("dobj", dobj, s);
					quantity.setContext("prep_of", prep_of, s);
					quantity.setContext("prep_in", prep_in, s);
					quantity.setContext("prep_to", prep_to, s);
					quantity.setContext("ccomp", ccomp, s);
					quantity.setContext("advmod", advmod, s);
					quantity.setContext("nmod", nmod, s);
					quantity.setContext("prep_in_amod", prep_in_amod, s);
					quantity.setContext("xcomp", xcomp, s);
					quantity.setContext("dep", dep, s);
					
					boolean isPart = false;
					if(s.hasToken(token.index()+1) && s.hasToken(token.index()+2)){
						String lemma1 = s.getLemma(token.index()+1);
						String lemma2 = s.getLemma(token.index()+2);
						
						if(POSUtil.isVerb(s.getPOS(token.index()+1)) || 
								POSUtil.isVerb(s.getPOS(token.index()+2))){
							if(lemma1.equalsIgnoreCase("be")|| lemma1.equalsIgnoreCase("has")
									||lemma1.equalsIgnoreCase("have"))
								isPart = true;
							if(lemma2.equalsIgnoreCase("be")|| lemma2.equalsIgnoreCase("has")
									||lemma2.equalsIgnoreCase("have"))
								isPart = true;
						}
						if(lemma1.equalsIgnoreCase("of")&&lemma2.equalsIgnoreCase("they")){
							isPart = true;
						}
					}
					
					if(isPart&&p.getNuberOfQuantities()>=2){
						int id = p.getNuberOfQuantities();
						
						Quantity q = p.getQuantities().get(id-2);
						quantity.setPart(true);
						quantity.setPartOf(q);
						
						
					}
					
					if(type.isEmpty()){
						if(prevType==null){
							int snid = 1;
							for(AnnotatedSentence sn: p.getAnnotatedSentences()){
								if(snid>sId)
									break;
								int right = -1;
								if(snid==sId)
									right =  token.index();
								prevType = this.typeDetecter.findObj(sn, right);
								if(!prevType.isEmpty())
									break;
							}
							type.addAll(prevType);
						}else{
							if(p.getNuberOfQuantities()>1){
								if(s.getLemma(token.index()+1).equalsIgnoreCase("of")){
									boolean added = false;
									for(int i=token.index()+2;i<s.getTokenSequence().size();i++){
										for(Quantity qu:p.getQuantities()){
											if(qu.getType()==null)
												continue;
											for(CoreLabel l: qu.getType()){
												if(l.lemma().equalsIgnoreCase(s.getLemma(i))){
													type.addAll(qu.getType());
													added = true;
													break;
												}
											}
											if(added) break;
										}
										if(added) break;
									}
									
									if(!added) type.addAll(prevType);
								}else{
									 type.addAll(prevType);
								}
							}else{
								type.addAll(prevType);
							}
						}

					}
					quantity.setType(type);
					prevType = type;
					//System.out.println("Quantity"+ quantity.getValue()+":"
						//	+ " "+ quantity.getType());

				}
			}

			if(isQuestion){
				Quantity quantity = null;
				ArrayList<Integer> typeIds = new ArrayList<Integer>();
				/**
				 * add the unknown
				 * assumes there is only one unknown currently
				 * TODO: update to work with multiple unknowns in a single question
				 */
				boolean how = false;
				boolean many = false;
				boolean much = false;
				boolean what = false;
				/***
				 * add what also in condition
				 */
				if(s.getRawSentence().toLowerCase().contains("how")||
						s.getRawSentence().toLowerCase().contains("what")){

					CoreLabel targetToken = null;
					CoreLabel q = null;
					for(CoreLabel token: s.getTokenSequence()){				
						if(how && many){
							if(POSUtil.isNoun(s.getPOS(token))||s.getPOS(token).toLowerCase().startsWith("jj")){
								//System.out.println("target "+ s.getLemma(targetToken));
								break;
							}
						}else if(how && much){
							break;
						}else if(s.getLemma(token).equalsIgnoreCase("how")){
							how = true;
							q = token;
						}else if(s.getLemma(token).equalsIgnoreCase("many")){
							many = true;
						}else if(how && !s.getLemma(token).equalsIgnoreCase("many")){
							much = true;
						}else if(s.getLemma(token).equalsIgnoreCase("what")){
							what = true;
							q = token;
						}else if(what){
							break;
						}
						targetToken = token;
					}

					quantity = p.addUnknown(sId,targetToken.index());
					//add type (closest noun phrase)					
					quantity.setType(this.typeDetecter.findType(targetToken, s, 
							prevType));
					ArrayList<CoreLabel> type = quantity.getType();
					
					
					typeIds.add(q.index());
					if(how)
						typeIds.add(targetToken.index()-1);//how
					for(CoreLabel l: type)
						typeIds.add(l.index());

					if(type.isEmpty()){

						type.addAll(prevType);
					}
					prevType = type;
				}

				Set<Integer> verb = this.associatedWordFinder.findAssociatedWordForQuestion(typeIds,s,
						"vb");
				Set<Integer> iobj = this.associatedWordFinder.findAssociatedWordWithRel(verb,
						typeIds, s, "iobj");
				Set<Integer> dobj = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "dobj");
				Set<Integer> nsubj = this.associatedWordFinder.findAssociatedWordWithRel(verb,
						typeIds, s, "nsubj");
				Set<Integer> prep_of = this.associatedWordFinder.findAssociatedWordWithRel(verb,
						typeIds, s, "prep_of");
				Set<Integer> prep_to = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "prep_to");
				Set<Integer> nmod = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "nmod");
				Set<Integer> tmod = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "tmod");
				Set<Integer> amod = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "amod");
				Set<Integer> dep = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "dep");
				
				Set<Integer> union =  new HashSet<Integer>();
				union.addAll(typeIds);
				union.addAll(prep_of);
				Set<Integer> prep_in = this.associatedWordFinder.findAssociatedWordWithRel(verb,
						new ArrayList<Integer>(union), s, "prep_in");
				Set<Integer> ccomp = this.associatedWordFinder.findAssociatedWordWithRel(verb, typeIds, s, "ccomp");
				Set<Integer> xcomp = this.associatedWordFinder.findAssociatedWordWithRel(verb,typeIds, s, "xcomp");
				union.clear();
				union.addAll(typeIds);
				union.addAll(ccomp);
				Set<Integer> advmod = this.associatedWordFinder.findAssociatedWordWithRel(verb,
						new ArrayList<Integer>(union), s, "advmod");
				Set<Integer> prep_in_amod = this.associatedWordFinder.findAssociatedWordWithRel(new HashSet<Integer>(),
						new ArrayList<Integer>(prep_in), s, "amod");
				quantity.setContext("verb", verb, s);
				quantity.setContext("nsubj", nsubj, s);
				quantity.setContext("amod", amod, s);
				quantity.setContext("iobj", iobj, s);
				quantity.setContext("prep_of", prep_of, s);
				quantity.setContext("prep_in", prep_in, s);
				quantity.setContext("prep_to", prep_to, s);
				quantity.setContext("ccomp", ccomp, s);
				quantity.setContext("advmod", advmod, s);
				quantity.setContext("nmod", nmod, s);
				quantity.setContext("dobj", dobj, s);
				quantity.setContext("tmod", tmod, s);
				quantity.setContext("prep_in_amod", prep_in_amod, s);
				quantity.setContext("xcomp", xcomp, s);
				quantity.setContext("dep", dep, s);
				
				//System.out.println("Quantity"+ quantity.getValue()+":"
					//	+ " "+ quantity.getType());
			}
		}
	}

	private boolean isAQuestion(AnnotatedSentence sentence){
		if(sentence.getRawSentence().contains("?"))
			return true;
		return false;
	}
}
