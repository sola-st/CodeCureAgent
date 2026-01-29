// ... all preceding code remains unchanged ...

        for (int i = 0; i < numSentences; ++i) {
          // reprocess candidates from previous iterations
          for (int it = firstIt; it < iteration; ++it) {
            for (int n = 0; n <= sizeOfNBest; ++n) {
              sents_str = inFile_sents[it].readLine();
              stats_str = inFile_stats[it].readLine();

              if (sents_str.equals("||||||")) {
                n = sizeOfNBest + 1;
              } else if (!existingCandStats.containsKey(sents_str)) {
                existingCandStats.put(sents_str, stats_str);
              } // if unseen candidate
            } // for (n)
          } // for (it)

          // copy relevant portion from mergedKnown to the merged file
          String line_mergedKnown = inFile_statsMergedKnown.readLine();
          while (!line_mergedKnown.equals("||||||")) {
            outFile_statsMerged.println(line_mergedKnown);
            line_mergedKnown = inFile_statsMergedKnown.readLine();
          }

          int[] stats = new int[suffStatsCount];

          for (int n = 0; n <= sizeOfNBest; ++n) {
            sents_str = inFile_sentsCurrIt.readLine();
            feats_str = inFile_featsCurrIt.readLine();

            if (sents_str.equals("||||||")) {
              n = sizeOfNBest + 1;
            } else if (!existingCandStats.containsKey(sents_str)) {

              if (!statsCurrIt_exists) {
                stats_str = inFile_statsCurrIt_unknown.readLine();

                String[] temp_stats = stats_str.split("\\s+");
                for (int s = 0; s < suffStatsCount; ++s) {
                  stats[s] = Integer.parseInt(temp_stats[s]);
                }

                outFile_statsCurrIt.println(stats_str);
              } else {
                stats_str = inFile_statsCurrIt.readLine();

                String[] temp_stats = stats_str.split("\\s+");
                for (int s = 0; s < suffStatsCount; ++s) {
                  stats[s] = Integer.parseInt(temp_stats[s]);
                }
              }

              outFile_statsMerged.println(stats_str);

              // save feats & stats
              // System.out.println(sents_str+" "+feats_str);

              feat_hash[i].put(sents_str, feats_str);
              stats_hash[i].put(sents_str, stats_str);

              featVal_str = feats_str.split("\\s+");

              if (feats_str.indexOf('=') != -1) {
                for (String featurePair : featVal_str) {
                  String[] pair = featurePair.split("=");
                  String name = pair[0];
                  int featId = Vocabulary.id(name);
                  // need to identify newly fired feats here
                  if (featId > numParams) {
                    ++numParams;
                    lambda.add(0d);
                  }
                }
              }
              existingCandStats.put(sents_str, stats_str);
              candCount[i] += 1;

              // newCandidatesAdded[iteration] += 1;
              // moved to code above detecting new candidates
            } else {
              if (statsCurrIt_exists)
                inFile_statsCurrIt.readLine();
              else {
                // write SS to outFile_statsCurrIt
                stats_str = existingCandStats.get(sents_str);
                outFile_statsCurrIt.println(stats_str);
              }
            }

          } // for (n)

          // now d = sizeUnknown_currIt[i] - 1

          if (statsCurrIt_exists)
            inFile_statsCurrIt.readLine();
          else
            outFile_statsCurrIt.println("||||||");

          existingCandStats.clear();
          totalCandidateCount += candCount[i];

          // output sentence progress
          if ((i + 1) % 500 == 0) {
            print((i + 1) + "\n" + "            ", 1);
          } else if ((i + 1) % 100 == 0) {
            print("+", 1);
          } else if ((i + 1) % 25 == 0) {
            print(".", 1);
          }

        } // for (i)

// ... all following code remains unchanged ...