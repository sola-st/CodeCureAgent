package uk.ac.bbsrc.tgac.miso.migration.destination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import uk.ac.bbsrc.tgac.miso.core.data.Box;
import uk.ac.bbsrc.tgac.miso.core.data.BoxSize;
import uk.ac.bbsrc.tgac.miso.core.data.BoxUse;
import uk.ac.bbsrc.tgac.miso.core.data.DetailedLibrary;
import uk.ac.bbsrc.tgac.miso.core.data.DetailedQcStatus;
import uk.ac.bbsrc.tgac.miso.core.data.DetailedSample;
import uk.ac.bbsrc.tgac.miso.core.data.Index;
import uk.ac.bbsrc.tgac.miso.core.data.Institute;
import uk.ac.bbsrc.tgac.miso.core.data.Instrument;
import uk.ac.bbsrc.tgac.miso.core.data.InstrumentModel;
import uk.ac.bbsrc.tgac.miso.core.data.Lab;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.LibraryDesign;
import uk.ac.bbsrc.tgac.miso.core.data.LibraryDesignCode;
import uk.ac.bbsrc.tgac.miso.core.data.Partition;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.ReferenceGenome;
import uk.ac.bbsrc.tgac.miso.core.data.Run;
import uk.ac.bbsrc.tgac.miso.core.data.Sample;
import uk.ac.bbsrc.tgac.miso.core.data.SampleAliquot;
import uk.ac.bbsrc.tgac.miso.core.data.SampleClass;
import uk.ac.bbsrc.tgac.miso.core.data.SamplePurpose;
import uk.ac.bbsrc.tgac.miso.core.data.SampleTissue;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerPartitionContainer;
import uk.ac.bbsrc.tgac.miso.core.data.Subproject;
import uk.ac.bbsrc.tgac.miso.core.data.TissueMaterial;
import uk.ac.bbsrc.tgac.miso.core.data.TissueOrigin;
import uk.ac.bbsrc.tgac.miso.core.data.TissueType;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryAliquot;
import uk.ac.bbsrc.tgac.miso.core.data.impl.TargetedSequencing;
import uk.ac.bbsrc.tgac.miso.core.data.impl.kit.KitDescriptor;
import uk.ac.bbsrc.tgac.miso.core.data.qc.LibraryQC;
import uk.ac.bbsrc.tgac.miso.core.data.qc.QcTarget;
import uk.ac.bbsrc.tgac.miso.core.data.qc.SampleQC;
import uk.ac.bbsrc.tgac.miso.core.data.type.LibrarySelectionType;
import uk.ac.bbsrc.tgac.miso.core.data.type.LibraryStrategyType;
import uk.ac.bbsrc.tgac.miso.core.data.type.LibraryType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.data.type.QcType;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;
import uk.ac.bbsrc.tgac.miso.migration.util.UniqueKeyHashMap;

/**
 * Used to look up entities by their alias, name, or other attributes when the ID is not known
 */
public class ValueTypeLookup {

  private Map<Long, SampleClass> sampleClassById;
  private Map<String, SampleClass> sampleClassByAlias;
  private Map<Long, TissueType> tissueTypeById;
  private Map<String, TissueType> tissueTypeByAlias;
  private Map<Long, TissueMaterial> tissueMaterialById;
  private Map<String, TissueMaterial> tissueMaterialByAlias;
  private Map<Long, KitDescriptor> kitById;
  private Map<String, KitDescriptor> kitByName;
  private Map<Long, SamplePurpose> samplePurposeById;
  private Map<String, SamplePurpose> samplePurposeByAlias;
  private Map<String, Institute> institutesByAlias;
  private Map<Long, Lab> labsById;
  private Map<Long, Map<String, Lab>> labsByInstituteId;
  private static final String UNSPECIFIED_LAB = "Not Specified";
  private Map<Long, TissueOrigin> tissueOriginsById;
  private Map<String, TissueOrigin> tissueOriginsByAlias;
  private Map<String, TissueOrigin> tissueOriginsByDescription;
  private Map<Long, LibrarySelectionType> librarySelectionsById;
  private Map<String, LibrarySelectionType> librarySelectionsByName;
  private Map<Long, LibraryStrategyType> libraryStrategiesById;
  private Map<String, LibraryStrategyType> libraryStrategiesByName;
  private Map<Long, LibraryType> libraryTypeById;
  private Map<PlatformType, Map<String, LibraryType>> libraryTypeByPlatformAndDescription;
  private Map<Long, LibraryDesign> libraryDesignById;
  private Map<String, Map<String, LibraryDesign>> libraryDesignBySampleClassAliasAndName;
  private Map<Long, LibraryDesignCode> libraryDesignCodeById;
  private Map<String, LibraryDesignCode> libraryDesignCodeByCode;
  private Map<Long, Index> indexById;
  private Map<String, Map<String, Index>> indexByFamilyAndSequence;
  private Map<Long, QcType> sampleQcTypeById;
  private Map<String, QcType> sampleQcTypeByName;
  private Map<Long, QcType> libraryQcTypeById;
  private Map<String, QcType> libraryQcTypeByName;
  private Map<Long, Instrument> instrumentById;
  private Map<String, Instrument> instrumentByName;
  private Map<Long, Subproject> subprojectById;
  private Map<String, Subproject> subprojectByAlias;
  private Map<Long, DetailedQcStatus> detailedQcStatusById;
  private Map<String, DetailedQcStatus> detailedQcStatusByDescription;
  private Map<String, ReferenceGenome> referenceGenomeByAlias;
  private Map<String, TargetedSequencing> targetedSequencingByAlias;
  private Map<String, BoxUse> boxUsesByAlias;
  private Collection<BoxSize> boxSizes;

  /**
   * Create a ValueTypeLookup loaded with data from the provided MisoServiceManager
   * 
   * @param misoServiceManager
   * @throws IOException if an there is an error pulling data from misoServiceManager
   */
  public ValueTypeLookup(MisoServiceManager misoServiceManager) throws IOException {
    setSampleClasses(misoServiceManager.getSampleClassDao().list());
    setTissueTypes(misoServiceManager.getTissueTypeDao().list());
    setTissueMaterials(misoServiceManager.getTissueMaterialDao().getTissueMaterial());
    setKits(misoServiceManager.getKitDao().listAllKitDescriptors());
    setSamplePurposes(misoServiceManager.getSamplePurposeDao().getSamplePurpose());
    setLabs(misoServiceManager.getLabDao().getLabs());
    setTissueOrigins(misoServiceManager.getTissueOriginDao().getTissueOrigin());
    setLibrarySelections(misoServiceManager.getLibrarySelectionService().list());
    setLibraryStrategies(misoServiceManager.getLibraryStrategyService().list());
    setLibraryTypes(misoServiceManager.getLibraryDao().listAllLibraryTypes());
    setLibraryDesigns(misoServiceManager.getLibraryDesignDao().list());
    setLibraryDesignCodes(misoServiceManager.getLibraryDesignCodeDao().list());
    setIndices(misoServiceManager.getIndexDao().list(0, 0, true, "id"));
    setSampleQcTypes(misoServiceManager.getQualityControlService().listQcTypes(QcTarget.Sample));
    setLibraryQcTypes(misoServiceManager.getQualityControlService().listQcTypes(QcTarget.Library));
    setSequencers(misoServiceManager.getInstrumentDao().listAll());
    setSubprojects(misoServiceManager.getSubprojectDao().getSubproject());
    setDetailedQcStatuses(misoServiceManager.getDetailedQcStatusDao().list());
    setReferenceGenomes(misoServiceManager.getReferenceGenomeService().list());
    setTargetedSequencings(misoServiceManager.getTargetedSequencingDao().list());
    setBoxUses(misoServiceManager.getBoxUseService().list());
    setBoxSizes(misoServiceManager.getBoxSizeService().list());
  }

  private void setSampleClasses(Collection<SampleClass> sampleClasses) {
    Map<Long, SampleClass> mapById = new UniqueKeyHashMap<>();
    Map<String, SampleClass> mapByAlias = new UniqueKeyHashMap<>();
    for (SampleClass sampleClass : sampleClasses) {
      mapByAlias.put(sampleClass.getAlias(), sampleClass);
      mapById.put(sampleClass.getId(), sampleClass);
    }
    this.sampleClassByAlias = mapByAlias;
    this.sampleClassById = mapById;
  }

  private void setTissueTypes(Collection<TissueType> tissueTypes) {
    Map<Long, TissueType> mapById = new UniqueKeyHashMap<>();
    Map<String, TissueType> mapByAlias = new UniqueKeyHashMap<>();
    for (TissueType tt : tissueTypes) {
      mapByAlias.put(tt.getAlias(), tt);
      mapById.put(tt.getId(), tt);
    }
    this.tissueTypeById = mapById;
    this.tissueTypeByAlias = mapByAlias;
  }

  private void setTissueMaterials(Collection<TissueMaterial> tissueMaterials) {
    Map<Long, TissueMaterial> mapById = new UniqueKeyHashMap<>();
    Map<String, TissueMaterial> mapByAlias = new UniqueKeyHashMap<>();
    for (TissueMaterial tm : tissueMaterials) {
      mapByAlias.put(tm.getAlias(), tm);
      mapById.put(tm.getId(), tm);
    }
    this.tissueMaterialById = mapById;
    this.tissueMaterialByAlias = mapByAlias;
  }

  private void setKits(Collection<KitDescriptor> kits) {
    Map<Long, KitDescriptor> mapById = new UniqueKeyHashMap<>();
    Map<String, KitDescriptor> mapByName = new UniqueKeyHashMap<>();
    for (KitDescriptor kit : kits) {
      mapByName.put(kit.getName(), kit);
      mapById.put(kit.getId(), kit);
    }
    this.kitById = mapById;
    this.kitByName = mapByName;
  }

  private void setSamplePurposes(Collection<SamplePurpose> samplePurposes) {
    Map<Long, SamplePurpose> mapById = new UniqueKeyHashMap<>();
    Map<String, SamplePurpose> mapByAlias = new UniqueKeyHashMap<>();
    for (SamplePurpose sp : samplePurposes) {
      mapByAlias.put(sp.getAlias(), sp);
      mapById.put(sp.getId(), sp);
    }
    this.samplePurposeById = mapById;
    this.samplePurposeByAlias = mapByAlias;
  }

  private void setLabs(Collection<Lab> labs) {
    Map<Long, Lab> labMapById = new UniqueKeyHashMap<>();
    Map<Long, Map<String, Lab>> labMapByInstituteId = new UniqueKeyHashMap<>();
    Map<String, Institute> instMapByAlias = new UniqueKeyHashMap<>();
    for (Lab lab : labs) {
      labMapById.put(lab.getId(), lab);
      if (labMapByInstituteId.get(lab.getInstitute().getId()) == null) {
        instMapByAlias.put(lab.getInstitute().getAlias(), lab.getInstitute());
        labMapByInstituteId.put(lab.getInstitute().getId(), new UniqueKeyHashMap<String, Lab>());
      }
      labMapByInstituteId.get(lab.getInstitute().getId()).put(lab.getAlias(), lab);
    }
    this.labsById = labMapById;
    this.labsByInstituteId = labMapByInstituteId;
    this.institutesByAlias = instMapByAlias;
  }

  private void setTissueOrigins(Collection<TissueOrigin> tissueOrigins) {
    Map<Long, TissueOrigin> mapById = new UniqueKeyHashMap<>();
    Map<String, TissueOrigin> mapByAlias = new UniqueKeyHashMap<>();
    Map<String, TissueOrigin> mapByDescription = new UniqueKeyHashMap<>();
    for (TissueOrigin to : tissueOrigins) {
      mapById.put(to.getId(), to);
      mapByAlias.put(to.getAlias(), to);
      mapByDescription.put(to.getDescription(), to);
    }
    this.tissueOriginsById = mapById;
    this.tissueOriginsByAlias = mapByAlias;
    this.tissueOriginsByDescription = mapByDescription;
  }

  private void setLibrarySelections(Collection<LibrarySelectionType> librarySelections) {
    Map<Long, LibrarySelectionType> mapById = new UniqueKeyHashMap<>();
    Map<String, LibrarySelectionType> mapByName = new UniqueKeyHashMap<>();
    for (LibrarySelectionType ls : librarySelections) {
      mapById.put(ls.getId(), ls);
      mapByName.put(ls.getName(), ls);
    }
    this.librarySelectionsById = mapById;
    this.librarySelectionsByName = mapByName;
  }

  private void setLibraryStrategies(Collection<LibraryStrategyType> libraryStrategies) {
    Map<Long, LibraryStrategyType> mapById = new UniqueKeyHashMap<>();
    Map<String, LibraryStrategyType> mapByName = new UniqueKeyHashMap<>();
    for (LibraryStrategyType ls : libraryStrategies) {
      mapById.put(ls.getId(), ls);
      mapByName.put(ls.getName(), ls);
    }
    this.libraryStrategiesById = mapById;
    this.libraryStrategiesByName = mapByName;
  }

  private void setLibraryTypes(Collection<LibraryType> libraryTypes) {
    Map<Long, LibraryType> mapById = new UniqueKeyHashMap<>();
    Map<PlatformType, Map<String, LibraryType>> mapByPlatformAndDesc = new UniqueKeyHashMap<>();
    for (LibraryType lt : libraryTypes) {
      if (!mapByPlatformAndDesc.containsKey(lt.getPlatformType())) {
        mapByPlatformAndDesc.put(lt.getPlatformType(), new UniqueKeyHashMap<String, LibraryType>());
      }
      mapByPlatformAndDesc.get(lt.getPlatformType()).put(lt.getDescription(), lt);
      mapById.put(lt.getId(), lt);
    }
    this.libraryTypeById = mapById;
    this.libraryTypeByPlatformAndDescription = mapByPlatformAndDesc;
  }

  private void setLibraryDesigns(Collection<LibraryDesign> libraryDesigns) {
    Map<Long, LibraryDesign> mapById = new UniqueKeyHashMap<>();
    Map<String, Map<String, LibraryDesign>> mapBySampleClassAliasAndName = new UniqueKeyHashMap<>();
    for (LibraryDesign ld : libraryDesigns) {
      Map<String, LibraryDesign> mapByName = mapBySampleClassAliasAndName.get(ld.getSampleClass().getAlias());
      if (mapByName == null) {
        mapByName = new UniqueKeyHashMap<>();
        mapBySampleClassAliasAndName.put(ld.getSampleClass().getAlias(), mapByName);
      }
      mapByName.put(ld.getName(), ld);
      mapById.put(ld.getId(), ld);
    }
    this.libraryDesignById = mapById;
    this.libraryDesignBySampleClassAliasAndName = mapBySampleClassAliasAndName;
  }

  private void setLibraryDesignCodes(Collection<LibraryDesignCode> libraryDesignCodes) {
    Map<Long, LibraryDesignCode> mapById = new UniqueKeyHashMap<>();
    Map<String, LibraryDesignCode> mapByCode = new UniqueKeyHashMap<>();
    for (LibraryDesignCode ldc : libraryDesignCodes) {
      mapByCode.put(ldc.getCode(), ldc);
      mapById.put(ldc.getId(), ldc);
    }
    this.libraryDesignCodeById = mapById;
    this.libraryDesignCodeByCode = mapByCode;
  }

  private void setIndices(Collection<Index> indices) {
    Map<Long, Index> mapById = new UniqueKeyHashMap<>();
    Map<String, Map<String, Index>> mapByFamilyAndSequence = new UniqueKeyHashMap<>();
    for (Index index : indices) {
      Map<String, Index> mapBySequence = mapByFamilyAndSequence.get(index.getFamily().getName());
      if (mapBySequence == null) {
        mapBySequence = new UniqueKeyHashMap<>();
        mapByFamilyAndSequence.put(index.getFamily().getName(), mapBySequence);
      }
      mapBySequence.put(index.getSequence(), index);
      mapById.put(index.getId(), index);
    }
    this.indexById = mapById;
    this.indexByFamilyAndSequence = mapByFamilyAndSequence;
  }

  private void setSampleQcTypes(Collection<QcType> qcTypes) {
    Map<Long, QcType> mapById = new UniqueKeyHashMap<>();
    Map<String, QcType> mapByName = new UniqueKeyHashMap<>();
    for (QcType qc : qcTypes) {
      mapByName.put(qc.getName(), qc);
      mapById.put(qc.getId(), qc);
    }
    this.sampleQcTypeById = mapById;
    this.sampleQcTypeByName = mapByName;
  }

  private void setLibraryQcTypes(Collection<QcType> qcTypes) {
    Map<Long, QcType> mapById = new UniqueKeyHashMap<>();
    Map<String, QcType> mapByName = new UniqueKeyHashMap<>();
    for (QcType qc : qcTypes) {
      mapByName.put(qc.getName(), qc);
      mapById.put(qc.getId(), qc);
    }
    this.libraryQcTypeById = mapById;
    this.libraryQcTypeByName = mapByName;
  }

  private void setSequencers(Collection<Instrument> sequencers) {
    Map<Long, Instrument> mapById = new UniqueKeyHashMap<>();
    Map<String, Instrument> mapByName = new UniqueKeyHashMap<>();
    for (Instrument sequencer : sequencers) {
      mapByName.put(sequencer.getName(), sequencer);
      mapById.put(sequencer.getId(), sequencer);
    }
    this.instrumentById = mapById;
    this.instrumentByName = mapByName;
  }

  private void setSubprojects(Collection<Subproject> subprojects) {
    Map<Long, Subproject> mapById = new UniqueKeyHashMap<>();
    Map<String, Subproject> mapByAlias = new UniqueKeyHashMap<>();
    for (Subproject subproject : subprojects) {
      mapByAlias.put(subproject.getAlias(), subproject);
      mapById.put(subproject.getId(), subproject);
    }
    this.subprojectById = mapById;
    this.subprojectByAlias = mapByAlias;
  }

  /**
   * Add a subproject to the lookup. Should be called when a new Subclass is saved and the same ValueTypeLookup is being used
   * 
   * @param subproject the new (already saved) Subproject
   */
  public void addSubproject(Subproject subproject) {
    if (!subproject.isSaved() || subproject.getAlias() == null)
      throw new IllegalArgumentException("Subproject is not saved");
    subprojectById.put(subproject.getId(), subproject);
    subprojectByAlias.put(subproject.getAlias(), subproject);
  }

  private void setDetailedQcStatuses(Collection<DetailedQcStatus> detailedQcStatuses) {
    Map<Long, DetailedQcStatus> mapById = new UniqueKeyHashMap<>();
    Map<String, DetailedQcStatus> mapByDesc = new UniqueKeyHashMap<>();
    for (DetailedQcStatus detailedQcStatus : detailedQcStatuses) {
      mapByDesc.put(detailedQcStatus.getDescription(), detailedQcStatus);
      mapById.put(detailedQcStatus.getId(), detailedQcStatus);
    }
    this.detailedQcStatusById = mapById;
    this.detailedQcStatusByDescription = mapByDesc;
  }

  private void setReferenceGenomes(Collection<ReferenceGenome> referenceGenomes) {
    Map<String, ReferenceGenome> mapByAlias = new UniqueKeyHashMap<>();
    for (ReferenceGenome referenceGenome : referenceGenomes) {
      mapByAlias.put(referenceGenome.getAlias(), referenceGenome);
    }
    this.referenceGenomeByAlias = mapByAlias;
  }

  private void setTargetedSequencings(Collection<TargetedSequencing> targetedSequencings) {
    Map<String, TargetedSequencing> mapByAlias = new UniqueKeyHashMap<>();
    for (TargetedSequencing tarSeq : targetedSequencings) {
      mapByAlias.put(tarSeq.getAlias(), tarSeq);
    }
    this.targetedSequencingByAlias = mapByAlias;
  }

  private void setBoxUses(Collection<BoxUse> boxUses) {
    Map<String, BoxUse> mapByAlias = new UniqueKeyHashMap<>();
    for (BoxUse boxUse : boxUses) {
      mapByAlias.put(boxUse.getAlias(), boxUse);
    }
    this.boxUsesByAlias = mapByAlias;
  }

  private void setBoxSizes(Collection<BoxSize> boxSizes) {
    this.boxSizes = boxSizes;
  }

  /**
   * Attempts to find an existing SampleClass
   * 
   * @param sampleClass a partially-formed SampleClass, which must have its ID or alias set in order for this method to resolve the
   *          SampleClass
   * @return the existing SampleClass if a matching one is found; null otherwise
   */
  @VisibleForTesting
  SampleClass resolve(SampleClass sampleClass) {
    if (sampleClass == null) return null;
    if (sampleClass.isSaved()) return sampleClassById.get(sampleClass.getId());
    if (sampleClass.getAlias() != null) return sampleClassByAlias.get(sampleClass.getAlias());
    return null;
  }

  public boolean isValidSampleClass(String alias) {
    return sampleClassByAlias.containsKey(alias);
  }

  /**
   * Attempts to find an existing TissueType
   * 
   * @param tissueType a partially-formed TissueType, which must have its ID or alias set in order for this method to resolve the TissueType
   * @return the existing TissueType if a matching one is found; null otherwise
   */
  @VisibleForTesting
  TissueType resolve(TissueType tissueType) {
    if (tissueType == null) return null;
    if (tissueType.isSaved()) return tissueTypeById.get(tissueType.getId());
    if (tissueType.getAlias() != null) return tissueTypeByAlias.get(tissueType.getAlias());
    return null;
  }

  public boolean isValidTissueType(String alias) {
    return tissueTypeByAlias.containsKey(alias);
  }

  /**
   * Attempts to find an existing TissueMaterial
   * 
   * @param tissueMaterial a partially-formed TissueMaterial, which must have its ID or alias set in order for this method to resolve the
   *          TissueMaterial
   * @return the existing TissueMaterial if a matching one is found; null otherwise
   */
  @VisibleForTesting
  TissueMaterial resolve(TissueMaterial tissueMaterial) {
    if (tissueMaterial == null) return null;
    if (tissueMaterial.isSaved()) return tissueMaterialById.get(tissueMaterial.getId());
    if (tissueMaterial.getAlias() != null) return tissueMaterialByAlias.get(tissueMaterial.getAlias());
    return null;
  }

  public boolean isValidTissueMaterial(String alias) {
    return tissueMaterialByAlias.containsKey(alias);
  }

  /**
   * Attempts to find an existing KitDescriptor
   * 
   * @param kit a partially-formed KitDescriptor, which must have its ID or name set in order for this method to resolve the KitDescriptor
   * @return the existing KitDescriptor if a matching one is found; null otherwise
   */
  @VisibleForTesting
  KitDescriptor resolve(KitDescriptor kit) {
    if (kit == null) return null;
    if (kit.isSaved()) return kitById.get(kit.getId());
    if (kit.getName() != null) return kitByName.get(kit.getName());
    return null;
  }

  public boolean isValidKitDescriptor(String name) {
    return kitByName.containsKey(name);
  }

  /**
   * Attempts to find an existing SamplePurpose
   * 
   * @param samplePurpose a partially-formed SamplePurpose, which must have its ID or alias set in order for this method to resolve the
   *          SamplePurpose
   * @return the existing SamplePurpose if a matching one is found; null otherwise
   */
  @VisibleForTesting
  SamplePurpose resolve(SamplePurpose samplePurpose) {
    if (samplePurpose == null) return null;
    if (samplePurpose.isSaved()) return samplePurposeById.get(samplePurpose.getId());
    if (samplePurpose.getAlias() != null) return samplePurposeByAlias.get(samplePurpose.getAlias());
    return null;
  }

  public boolean isValidSamplePurpose(String alias) {
    return samplePurposeByAlias.containsKey(alias);
  }

  /**
   * Attempts to find an existing Lab
   * 
   * @param lab a partially-formed Lab, which must have its ID, institute ID, or institute alias set in order for this method to resolve the
   *          Lab. The Lab's alias is used as well; if neither Lab ID nor Lab alias are set, the Lab alias 'Not Specified' is assumed, and
   *          the
   *          Institute is first resolved by ID or alias
   * @return the existing Lab if a matching one is found; null otherwise
   */
  @VisibleForTesting
  Lab resolve(Lab lab) {
    if (lab == null) return null;
    if (lab.isSaved()) return labsById.get(lab.getId());
    if (lab.getInstitute() != null) {
      Institute inst = lab.getInstitute();
      if (!lab.getInstitute().isSaved() && lab.getInstitute().getAlias() != null) {
        Institute i = institutesByAlias.get(lab.getInstitute().getAlias());
        if (i != null) inst = i;
      }
      if (inst.isSaved()) {
        String labAlias = lab.getAlias();
        if (labAlias == null) labAlias = UNSPECIFIED_LAB;
        Map<String, Lab> labsByAlias = labsByInstituteId.get(inst.getId());
        if (labsByAlias == null) return null;
        return labsByAlias.get(labAlias);
      }
    }
    return null;
  }

  public boolean isValidLab(String labAlias, String instituteAlias) {
    return institutesByAlias.containsKey(instituteAlias)
        && labsByInstituteId.get(institutesByAlias.get(instituteAlias).getId()).containsKey(labAlias);
  }

  /**
   * Attempts to find an existing TissueOrigin
   * 
   * @param tissueOrigin a partially-formed TissueOrigin, which must have its ID, alias, or description set in order for this method to
   *          resolve the TissueOrigin
   * @return the existing TissueOrigin if a matching one is found; null otherwise
   */
  public TissueOrigin resolve(TissueOrigin tissueOrigin) {
    if (tissueOrigin == null) return null;
    if (tissueOrigin.isSaved()) return tissueOriginsById.get(tissueOrigin.getId());
    if (tissueOrigin.getAlias() != null) {
      TissueOrigin byAlias = tissueOriginsByAlias.get(tissueOrigin.getAlias());
      if (byAlias != null) return byAlias;
    }
    if (tissueOrigin.getDescription() != null) return tissueOriginsByDescription.get(tissueOrigin.getDescription());
    return null;
  }

  public boolean isValidTissueOrigin(String aliasOrDescription) {
    return tissueOriginsByAlias.containsKey(aliasOrDescription) || tissueOriginsByDescription.containsKey(aliasOrDescription);
  }

  /**
   * Attempts to find an existing LibrarySelectionType
   * 
   * @param librarySelectionType a partially-formed LibrarySelectionType, which must have its ID or name set in order for this method to
   *          resolve the LibrarySelectionType
   * @return the existing LibrarySelectionType if a matching one is found; null otherwise
   */
  @VisibleForTesting
  LibrarySelectionType resolve(LibrarySelectionType librarySelectionType) {
    if (librarySelectionType == null) return null;
    if (librarySelectionType.isSaved()) {
      return librarySelectionsById.get(librarySelectionType.getId());
    }
    if (librarySelectionType.getName() != null) return librarySelectionsByName.get(librarySelectionType.getName());
    return null;
  }

  public boolean isValidLibrarySelectionType(String name) {
    return librarySelectionsByName.containsKey(name);
  }

  /**
   * Attempts to find an existing LibraryStrategyType
   * 
   * @param libraryStrategyType a partially-formed LibraryStrategyType, which must have its ID or name set in order for this method to
   *          resolve the LibraryStrategyType
   * @return the existing LibraryStrategyType if a matching one is found; null otherwise
   */
  @VisibleForTesting
  LibraryStrategyType resolve(LibraryStrategyType libraryStrategyType) {
    if (libraryStrategyType == null) return null;
    if (libraryStrategyType.isSaved()) {
      return libraryStrategiesById.get(libraryStrategyType.getId());
    }
    if (libraryStrategyType.getName() != null) return libraryStrategiesByName.get(libraryStrategyType.getName());
    return null;
  }

  public boolean isValidLibraryStrategyType(String name) {
    return libraryStrategiesByName.containsKey(name);
  }

  /**
   * Attempts to find an existing LibraryType
   * 
   * @param libraryType a partially-formed LibraryType, which must have its ID or platform AND description set in order for this method to
   *          resolve the LibraryType
   * @return the existing LibraryType if a matching one is found; null otherwise
   */
  @VisibleForTesting
  LibraryType resolve(LibraryType libraryType) {
    if (libraryType == null) return null;
    if (libraryType.isSaved()) return libraryTypeById.get(libraryType.getId());
    if (libraryType.getDescription() != null && libraryType.getPlatformType() != null) {
      Map<String, LibraryType> mapByDesc = libraryTypeByPlatformAndDescription.get(libraryType.getPlatformType());
      return mapByDesc == null ? null : mapByDesc.get(libraryType.getDescription());
    }
    return null;
  }

  public boolean isValidLibraryType(String description, String platformType) {
    return libraryTypeByPlatformAndDescription.containsKey(platformType)
        && libraryTypeByPlatformAndDescription.get(platformType).containsKey(description);
  }

  /**
   * Attempts to find an existing LibraryDesign
   * 
   * @param libraryDesign a partially-formed LibraryDesign, which must have its ID or name set in order for this method to resolve the
   *          LibraryDesign
   * @return the existing LibraryDesign if a matching one is found; null otherwise
   */
  @VisibleForTesting
  LibraryDesign resolve(LibraryDesign libraryDesign) {
    if (libraryDesign == null) return null;
    if (libraryDesign.isSaved()) return libraryDesignById.get(libraryDesign.getId());
    if (libraryDesign.getSampleClass() != null && libraryDesign.getSampleClass().getAlias() != null && libraryDesign.getName() != null) {
      Map<String, LibraryDesign> mapByName = libraryDesignBySampleClassAliasAndName.get(libraryDesign.getSampleClass().getAlias());
      return mapByName == null ? null : mapByName.get(libraryDesign.getName());
    }
    return null;
  }

  /**
   * Attempts to find an existing LibraryDesignCode
   * 
   * @param LibraryDesignCode
   *          a partially-formed LibraryDesignCode, which must have either its ID or its code set in order for this method to resolve the
   *          LibraryDesignCode
   * @return the existing LibraryDesignCode if a matching one is found; null otherwise
   */
  @VisibleForTesting
  LibraryDesignCode resolve(LibraryDesignCode libraryDesignCode) {
    if (libraryDesignCode == null) return null;
    if (libraryDesignCode.isSaved()) return libraryDesignCodeById.get(libraryDesignCode.getId());
    if (libraryDesignCode.getCode() != null) return libraryDesignCodeByCode.get(libraryDesignCode.getCode());
    return null;
  }

  /**
   * Attempts to find an existing Index
   * 
   * @param index a partially-formed Index, which must have either its ID or its sequence AND family name set in order for this method to
   *          resolve the Index
   * @return the existing Index if a matching one is found; null otherwise
   */
  @VisibleForTesting
  Index resolve(Index index) {
    if (index == null) return null;
    if (index.isSaved()) return indexById.get(index.getId());
    if (index.getFamily() != null && index.getFamily().getName() != null && index.getSequence() != null) {
      Map<String, Index> mapBySequence = indexByFamilyAndSequence.get(index.getFamily().getName());
      return mapBySequence == null ? null : mapBySequence.get(index.getSequence());
    }
    return null;
  }

  public boolean isValidIndex(String familyName, String sequence) {
    return indexByFamilyAndSequence.containsKey(familyName)
```java
            String labAlias = null;
            String instituteAlias = null;

    if (referenceGenome.getAlias() != null) return referenceGenomeByAlias.get(referenceGenome.getAlias());
    return null;
  }

}