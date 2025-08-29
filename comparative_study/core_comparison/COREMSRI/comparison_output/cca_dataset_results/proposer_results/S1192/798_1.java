```java
package uk.ac.bbsrc.tgac.miso.service.impl;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.isStringEmptyOrNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.ac.bbsrc.tgac.miso.core.data.Barcodable;
import uk.ac.bbsrc.tgac.miso.core.data.Boxable;
import uk.ac.bbsrc.tgac.miso.core.data.ConcentrationUnit;
import uk.ac.bbsrc.tgac.miso.core.data.HierarchyEntity;
import uk.ac.bbsrc.tgac.miso.core.data.Identifiable;
import uk.ac.bbsrc.tgac.miso.core.data.Nameable;
import uk.ac.bbsrc.tgac.miso.core.data.VolumeUnit;
import uk.ac.bbsrc.tgac.miso.core.data.impl.view.BarcodableReference;
import uk.ac.bbsrc.tgac.miso.core.service.BarcodableReferenceService;
import uk.ac.bbsrc.tgac.miso.core.service.ProviderService;
import uk.ac.bbsrc.tgac.miso.core.service.exception.ValidationError;
import uk.ac.bbsrc.tgac.miso.core.service.exception.ValidationException;
import uk.ac.bbsrc.tgac.miso.core.service.naming.NamingScheme;
import uk.ac.bbsrc.tgac.miso.core.service.naming.validation.ValidationResult;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;

public class ValidationUtils {

  private ValidationUtils() {
    throw new IllegalStateException("Static util class not intended for instantiation");
  }

  private static final String ERROR_NOTE_CANNOT_BE_SPECIFIED_WITHOUT_STATUS = "Note cannot be specified without status";
  private static final String ERROR_NOTE_MUST_BE_SPECIFIED_FOR_STATUS = "Note must be specified for the selected status";
  private static final String ERROR_NOTE_CANNOT_BE_SPECIFIED_FOR_STATUS = "Note cannot be specified for the selected status";
  private static final String ERROR_CONCENTRATION_UNITS_MUST_BE_SPECIFIED = "Concentration units must be specified";
  private static final String ERROR_VOLUME_UNITS_MUST_BE_SPECIFIED = "Volume units must be specified";
  private static final String ERROR_MUST_BE_SPECIFIED = "Must be specified";
  private static final String INVALID_URL = "URL is not valid";

  public static <T extends Barcodable> void validateBarcodeUniqueness(T barcodable, T beforeChange, BarcodableReferenceService service,
      Collection<ValidationError> errors) throws IOException {
    if (barcodable.getIdentificationBarcode() != null
        && (beforeChange == null || !barcodable.getIdentificationBarcode().equals(beforeChange.getIdentificationBarcode()))) {
      BarcodableReference ref = service.checkForExisting(barcodable.getIdentificationBarcode());
      if (ref != null) {
        errors.add(new ValidationError("identificationBarcode",
            String.format("%s '%s' already has this barcode", ref.getEntityType(), ref.getFullLabel())));
      }
    }
  }

  public static void validateConcentrationUnits(BigDecimal concentration, ConcentrationUnit units, String field, String label,
      Collection<ValidationError> errors) {
    if (concentration != null && units == null) {
      errors.add(new ValidationError(field, label + " units must be specified"));
    }
  }

  public static void validateConcentrationUnits(BigDecimal concentration, ConcentrationUnit units, Collection<ValidationError> errors) {
    if (concentration != null && concentration.compareTo(BigDecimal.ZERO) > 0 && units == null) {
      errors.add(new ValidationError("concentrationUnits", ERROR_CONCENTRATION_UNITS_MUST_BE_SPECIFIED));
    }
  }

  public static void validateVolumeUnits(BigDecimal volume, VolumeUnit units, Collection<ValidationError> errors) {
    if (volume != null && volume.compareTo(BigDecimal.ZERO) > 0 && units == null) {
      errors.add(new ValidationError("volumeUnits", ERROR_VOLUME_UNITS_MUST_BE_SPECIFIED));
    }
  }

  public static void validateUnboxableFields(Boxable item, Collection<ValidationError> errors) {
    if (item.getBox() != null) {
      if (item.isDiscarded()) errors.add(new ValidationError(ERROR_MUST_BE_SPECIFIED));
      if (item.getDistributionTransfer() != null) errors.add(new ValidationError(ERROR_MUST_BE_SPECIFIED));
    }
  }

  public static void validateDetailedQcStatus(HierarchyEntity item, Collection<ValidationError> errors) {
    if (item.getDetailedQcStatus() == null) {
      if (item.getDetailedQcStatusNote() != null) {
        errors.add(new ValidationError("detailedQcStatusNote", ERROR_NOTE_CANNOT_BE_SPECIFIED_WITHOUT_STATUS));
      }
    } else if (item.getDetailedQcStatus().getNoteRequired()) {
      if (LimsUtils.isStringEmptyOrNull(item.getDetailedQcStatusNote())) {
        errors.add(new ValidationError("detailedQcStatusNote", ERROR_NOTE_MUST_BE_SPECIFIED_FOR_STATUS));
      }
    } else if (item.getDetailedQcStatusNote() != null) {
      errors.add(new ValidationError("detailedQcStatusNote", ERROR_NOTE_CANNOT_BE_SPECIFIED_FOR_STATUS));
    }
  }

  public static void validateUrl(String fieldName, String maybeUrl, boolean allowEmptyUrl, Collection<ValidationError> errors) {
    if (isStringEmptyOrNull(maybeUrl) && allowEmptyUrl) return;
    URL url = parseUrl(maybeUrl);
    if (url == null) {
      errors.add(new ValidationError(fieldName, INVALID_URL));
      return;
    }
    if (!allowedUrlSchemes.contains(url.getProtocol())) {
      errors.add(new ValidationError(fieldName, INVALID_URL));
    }
  }

  private static URL parseUrl(String maybeUrl) {
    try {
      return new URL(maybeUrl);
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private static Set<String> allowedUrlSchemes = new HashSet<>();
  static {
    allowedUrlSchemes.add("http");
    allowedUrlSchemes.add("https");
  }

  public static void validateNameOrThrow(Nameable object, NamingScheme namingScheme) {
    ValidationResult val = namingScheme.validateName(object.getName());
    if (!val.isValid()) {
      throw new ValidationException(new ValidationError("name", val.getMessage()));
    }
  }

  public static <T, R> boolean isSetAndChanged(Function<T, R> getter, T newItem, T beforeChange) {
    R after = getter.apply(newItem);
    if (after == null) {
      return false;
    } else if (beforeChange == null) {
      return true;
    }
    R before = getter.apply(beforeChange);
    return !after.equals(before);
  }

  public static <T, R> boolean isChanged(Function<T, R> getter, T newItem, T beforeChange) {
    if (beforeChange == null) {
      return true;
    }
    R after = getter.apply(newItem);
    R before = getter.apply(beforeChange);
    if (after == null) {
      return before != null;
    } else {
      return !after.equals(before);
    }
  }

  public static <T extends Identifiable> void loadChildEntity(Consumer<T> setter, T childEntity, ProviderService<T> service,
      String property)
      throws IOException {
    if (childEntity != null) {
      T item = service.get(childEntity.getId());
      if (item == null) {
        throw new ValidationException(new ValidationError(property, "Invalid item ID: " + childEntity.getId()));
      }
      setter.accept(item);
    }
  }

  public static <T extends Identifiable> void applySetChanges(Set<T> to, Set<T> from) {
    to.removeIf(toItem -> from.stream().noneMatch(fromItem -> fromItem.getId() == toItem.getId()));
    from.forEach(fromItem -> {
      if (to.stream().noneMatch(toItem -> toItem.getId() == fromItem.getId())) {
        to.add(fromItem);
      }
    });
  }

  public static ValidationException rewriteParentErrors(ValidationException original) {
    return new ValidationException(original.getErrors().stream()
        .map(err -> new ValidationError(String.format("Parent %s: %s", err.getProperty(), err.getMessage())))
        .collect(Collectors.toList()));
  }

  public static ValidationError makeNoNullError(String property) {
    return new ValidationError(property, ERROR_MUST_BE_SPECIFIED);
  }

}
